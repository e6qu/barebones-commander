/**
 * This file is part of muCommander, http://www.mucommander.com
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.barebones.commander.commons.file.protocol.local;

import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.file.FileFactory;
import dev.barebones.commander.commons.file.FileOperation;
import dev.barebones.commander.commons.file.FilePermissions;
import dev.barebones.commander.commons.file.FileURL;
import dev.barebones.commander.commons.file.GroupedPermissionBits;
import dev.barebones.commander.commons.file.IndividualPermissionBits;
import dev.barebones.commander.commons.file.MacOsSystemFolder;
import dev.barebones.commander.commons.file.MonitoredFile;
import dev.barebones.commander.commons.file.PermissionAccess;
import dev.barebones.commander.commons.file.PermissionBits;
import dev.barebones.commander.commons.file.PermissionType;
import dev.barebones.commander.commons.file.UnsupportedFileOperation;
import dev.barebones.commander.commons.file.UnsupportedFileOperationException;
import dev.barebones.commander.commons.file.filter.FilenameFilter;
import dev.barebones.commander.commons.file.protocol.ProtocolFile;
import dev.barebones.commander.commons.file.util.PathUtils;
import dev.barebones.commander.commons.io.BufferPool;
import dev.barebones.commander.commons.io.FileUtils;
import dev.barebones.commander.commons.io.FilteredOutputStream;
import dev.barebones.commander.commons.io.RandomAccessInputStream;
import dev.barebones.commander.commons.io.RandomAccessOutputStream;
import dev.barebones.commander.commons.runtime.OsFamily;
import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * LocalFile provides access to files located on a locally-mounted filesystem. Note that despite the class' name,
 * LocalFile instances may indifferently be residing on a local hard drive, or on a remote server mounted locally by the
 * operating system.
 *
 * <p>
 * The associated {@link FileURL} scheme is {@link #SCHEMA}. The host part should be {@link FileURL#LOCALHOST}. Native
 * path separators can be used in the path part.
 *
 * <p>
 * Here are a few examples of valid local file URLs: <code>
 * file://localhost/usr/bin/gcc<br>
 * file://localhost/~<br>
 * file://home/maxence/..<br>
 * </code>
 *
 * <p>
 * Access to local files is provided by the <code>java.io</code> API, {@link #getUnderlyingFileObject()} allows to
 * retrieve an <code>java.io.File</code> instance corresponding to this LocalFile.
 *
 * @author Maxence Bernard
 */
public class LocalFile extends ProtocolFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFile.class);

    final protected File file;
    private FilePermissions permissions;

    /** Absolute file path, free of trailing separator */
    final protected String absPath;

    /** Caches the parent folder, initially null until getParent() gets called */
    protected AbstractFile parent;
    /** Indicates whether the parent folder instance has been retrieved and cached or not (parent can be null) */
    protected boolean parentValueSet;

    /** Underlying local filesystem's path separator. */
    public final static String SEPARATOR = File.separator;

    /** The corresponding schema part of these files in {@link FileURL} */
    public final static String SCHEMA = "file";

    /** Changeable permissions mask: rwx------ (700 octal). */
    private final static PermissionBits CHANGEABLE_PERMISSIONS = new GroupedPermissionBits(448);

    private String owner, group;

    /**
     * List of known UNIX filesystems.
     */
    public static final String[] KNOWN_UNIX_FS = { "adfs", "affs", "autofs", "btrfs", "cifs", "coda", "cramfs",
            "debugfs", "efs", "ext2", "ext3", "ext4", "fuseblk", "hfs", "hfsplus", "hpfs",
            "iso9660", "jfs", "minix", "msdos", "ncpfs", "nfs", "nfs4", "ntfs",
            "qnx4", "reiserfs", "smbfs", "udf", "ufs", "usbfs", "vfat", "xfs" };

    /**
     * Creates a new instance of LocalFile and a corresponding {@link File} instance.
     */
    protected LocalFile(FileURL fileURL) throws IOException {
        this(fileURL, null);
    }

    /**
     * Creates a new instance of LocalFile, using the given {@link File} if not <code>null</code>, creating a new
     * {@link File} instance otherwise.
     */
    protected LocalFile(FileURL fileURL, File file) throws IOException {
        super(fileURL);

        String absPath;
        if (file == null) {
            String path = fileURL.getPath();
            // see https://github.com/mucommander/mucommander/issues/898
            if (OsFamily.MAC_OS.isCurrent()) {
                path = FileUtils.normalizeWithNFD(path);
            }

            // Create the java.io.File instance and throw an exception if the path is not absolute.
            file = new File(path);
            if (!file.isAbsolute()) {
                throw new IOException();
            }

            absPath = file.getAbsolutePath();

            // Remove the trailing separator if present
            if (absPath.endsWith(SEPARATOR)) {
                absPath = absPath.substring(0, absPath.length() - 1);
            }
        }
        // the java.io.File instance was created by ls(), no need to re-create it or call the costly
        // File#getAbsolutePath()
        else {
            absPath = fileURL.getPath();
            // see https://github.com/mucommander/mucommander/issues/898
            if (OsFamily.MAC_OS.isCurrent()) {
                absPath = FileUtils.normalizeWithNFD(absPath);
            }

        }

        this.absPath = absPath;
        this.file = file;
        this.permissions = new LocalFilePermissions(file);
    }

    ////////////////////////////////
    // LocalFile-specific methods //
    ////////////////////////////////

    /**
     * Returns the user home folder. Most if not all OSes have one, but in the unlikely event that the OS doesn't have
     * one or that the folder cannot be resolved, <code>null</code> will be returned.
     *
     * @return the user home folder
     */
    public static AbstractFile getUserHome() {
        String userHomePath = System.getProperty("user.home");
        if (userHomePath == null) {
            return null;
        }

        return FileFactory.getFile(userHomePath);
    }

    /**
     * Resolves and returns all local volumes:
     * <ul>
     * <li>On UNIX-based OSes, these are the mount points declared in <code>/etc/ftab</code>.</li>
     * </ul>
     * <p>
     * The return list of volumes is purposively not cached so that new volumes will be returned as soon as they are
     * mounted.
     * </p>
     *
     * @return all local volumes
     */
    public static AbstractFile[] getVolumes() {
        Set<AbstractFile> volumes = new HashSet<>();

        // Add Mac OS X's /Volumes subfolders and not file roots ('/') since Volumes already contains a named link
        // (like 'Hard drive' or whatever silly name the user gave his primary hard disk) to /
        if (OsFamily.MAC_OS.isCurrent()) {
            addMacOSXVolumes(volumes);
        } else {
            // Add java.io.File's root folders
            addJavaIoFileRoots(volumes);

            // Add /proc/mounts folders under UNIX-based systems.
            if (OsFamily.getCurrent().isUnixBased()) {
                addMountEntries(volumes);
            }
        }

        AbstractFile homeFolder = getUserHome();
        if (homeFolder != null) {
            volumes.add(homeFolder);
        }

        addDesktopEntry(volumes, homeFolder);

        return volumes.toArray(AbstractFile[]::new);
    }

    @Override
    public MonitoredFile toMonitoredFile() {
        return new LocalMonitoredFile(this);
    }

    ////////////////////
    // Helper methods //
    ////////////////////

    /**
     * Resolves the root folders returned by {@link FileSystem#getRootDirectories()} and adds them to the given <code>Vector</code>.
     *
     * @param volumes the <code>Vector</code> to add root folders to
     */
    private static void addJavaIoFileRoots(Set<AbstractFile> volumes) {
        for (Path path : FileSystems.getDefault().getRootDirectories()) {
            try {
                volumes.add(FileFactory.getFile(path.toFile().getAbsolutePath(), true));
            } catch (IOException e) {
                LOGGER.trace(e.getMessage());
            }
        }
    }

    /**
     * Parses the <code>/proc/mounts</code> kernel virtual file, resolves all the mount points that look like regular
     * filesystems it contains and adds them to the given <code>Vector</code>.
     *
     * @param volumes the <code>Vector</code> to add mount points to
     */
    private static void addMountEntries(Set<AbstractFile> volumes) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream("/proc/mounts"), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            // read each line in file and parse it
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // split line into tokens separated by " \t\n\r\f"
                // tokens are: device, mount_point, fs_type, attributes, fs_freq, fs_passno
                StringTokenizer st = new StringTokenizer(line);
                st.nextToken();
                String mountPoint = st.nextToken().replace("\\040", " ");
                String fsType = st.nextToken();
                // check whether this is really a known physical FS
                boolean knownFS = Arrays.stream(KNOWN_UNIX_FS).anyMatch(fs -> fs.equals(fsType));
                if (knownFS) {
                    AbstractFile file = FileFactory.getFile(mountPoint);
                    if (file != null)
                        volumes.add(file);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing /proc/mounts entries", e);
        }
    }

    /**
     * Adds Desktop to the given volumes entries if home directory is defined and Desktop folder
     * is present and is writable (~/Desktop)
     * @param volumes the <code>Vector</code> to add mount points to
     * @param homeFolder  a home folder, can be null
     */
    private static void addDesktopEntry(Set<AbstractFile> volumes, AbstractFile homeFolder) {
        if (homeFolder == null) {
            return;
        }
        try {
            AbstractFile desktop = homeFolder.getDirectChild("Desktop");
            if (desktop.exists() && desktop.isDirectory() && desktop.canRead()) {
                volumes.add(desktop);
            }
        } catch (IOException e) {
            LOGGER.debug("Thrown exception while getting Desktop folder", e);
        }
    }

    /**
     * Adds all <code>/Volumes</code> subfolders to the given <code>Vector</code>.
     *
     * @param volumes the <code>Vector</code> to add the volumes to
     */
    private static void addMacOSXVolumes(Set<AbstractFile> volumes) {
        // /Volumes not resolved for some reason, giving up
        AbstractFile volumesFolder = FileFactory.getFile("/Volumes");
        if (volumesFolder == null) {
            return;
        }

        // Adds subfolders
        try {
            AbstractFile volumesFiles[] = volumesFolder.ls();
            Arrays.stream(volumesFiles).filter(AbstractFile::isDirectory).forEach(volumes::add);
        } catch (IOException e) {
            LOGGER.warn("Can't get /Volumes subfolders", e);
        }
    }

    /////////////////////////////////
    // AbstractFile implementation //
    /////////////////////////////////

    /**
     * Returns a <code>java.io.File</code> instance corresponding to this file.
     */
    @Override
    public Object getUnderlyingFileObject() {
        return file;
    }

    @Override
    public boolean isSymlink() {
        return Files.isSymbolicLink(file.toPath());
    }

    @Override
    public boolean isSystem() {
        if (OsFamily.MAC_OS.isCurrent()) {
            return MacOsSystemFolder.isSystemFile(this);
        }
        return false;
    }

    @Override
    public long getDate() {
        return file.lastModified();
    }

    @Override
    public void changeDate(long lastModified) throws IOException {
        // java.io.File#setLastModified(long) throws an IllegalArgumentException if time is negative.
        // If specified time is negative, set it to 0 (01/01/1970).
        if (lastModified < 0) {
            lastModified = 0;
        }

        if (!file.setLastModified(lastModified)) {
            throw new IOException();
        }
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public AbstractFile getParent() {
        // Retrieve the parent AbstractFile instance and cache it
        if (!parentValueSet) {
            if (!isRoot()) {
                FileURL parentURL = getURL().getParent();
                if (parentURL != null) {
                    parent = FileFactory.getFile(parentURL);
                }
            }
            parentValueSet = true;
        }
        return parent;
    }

    @Override
    public void setParent(AbstractFile parent) {
        this.parent = parent;
        this.parentValueSet = true;
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public FilePermissions getPermissions() {
        return permissions;
    }

    @Override
    public PermissionBits getChangeablePermissions() {
        return CHANGEABLE_PERMISSIONS;
    }

    @Override
    public AbstractFile getChild(String relativePath, AbstractFile template) throws IOException {
        return super.getChild(relativePath, template);
    }

    @Override
    public void changePermission(PermissionAccess access, PermissionType permission, boolean enabled)
            throws IOException {
        // Only the 'user' permissions are supported
        if (access != PermissionAccess.USER) {
            throw new IOException();
        }

        boolean success = false;
        switch (permission) {
        case READ:
            success = file.setReadable(enabled);
            break;
        case WRITE:
            success = file.setWritable(enabled);
            break;
        case EXECUTE:
            success = file.setExecutable(enabled);
        }

        if (!success) {
            throw new IOException();
        }
    }

    @Override
    public String getOwner() {
        if (owner != null) {
            return owner;
        }
        try {
            owner = Files.getOwner(file.toPath()).getName();
        } catch (NoSuchFileException e) {
            LOGGER.info("failed to get owner of {}, either doesn't exist anymore or is protected by the system", file);
        } catch (IOException e) {
            LOGGER.error("failed to get owner of {}", file, e);
        }
        return owner;
    }

    @Override
    public boolean canGetOwner() {
        if (owner != null) {
            return true;
        }
        try {
            return Files.getFileStore(file.toPath()).supportsFileAttributeView(FileOwnerAttributeView.class);
        } catch (Exception e) {
            LOGGER.trace(e.getMessage());
            return false;
        }
    }

    @Override
    public String getGroup() {
        if (group != null) {
            return group;
        }
        try {
            var attributes = Files.readAttributes(file.toPath(), PosixFileAttributes.class);
            group = attributes.group().getName();
        } catch (IOException e) {
            if (e instanceof NoSuchFileException) {
                LOGGER.info("Failed to get group of {}, either doesn't exist anymore or is protected by the system", file);
            } else {
                LOGGER.error("Failed to get group of {}", file, e);
            }
        }
        return group;
    }

    @Override
    public boolean canGetGroup() {
        if (group != null) {
            return true;
        }
        try {
            return Files.getFileStore(file.toPath()).supportsFileAttributeView(PosixFileAttributeView.class);
        } catch (IOException e) {
            LOGGER.trace(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    /**
     * Implementation notes: the returned <code>InputStream</code> uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        // Hold the FileInputStream in a local so we can close it if
        // getChannel() throws — otherwise the underlying FD leaks.
        FileInputStream fis = new FileInputStream(file);
        try {
            return new LocalInputStream(fis.getChannel());
        } catch (RuntimeException | Error e) {
            try { fis.close(); } catch (IOException closeEx) { e.addSuppressed(closeEx); }
            throw e;
        }
    }

    /**
     * Implementation notes: the returned <code>InputStream</code> uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        FileOutputStream fos = new FileOutputStream(absPath, false);
        try {
            return new LocalOutputStream(fos.getChannel());
        } catch (RuntimeException | Error e) {
            try { fos.close(); } catch (IOException closeEx) { e.addSuppressed(closeEx); }
            throw e;
        }
    }

    /**
     * Implementation notes: the returned <code>InputStream</code> uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     */
    @Override
    public OutputStream getAppendOutputStream() throws IOException {
        FileOutputStream fos = new FileOutputStream(absPath, true);
        try {
            return new LocalOutputStream(fos.getChannel());
        } catch (RuntimeException | Error e) {
            try { fos.close(); } catch (IOException closeEx) { e.addSuppressed(closeEx); }
            throw e;
        }
    }

    /**
     * Implementation notes: the returned <code>InputStream</code> uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     */
    @Override
    public RandomAccessInputStream getRandomAccessInputStream() throws IOException {
        return new LocalRandomAccessInputStream(new RandomAccessFile(file, "r").getChannel());
    }

    /**
     * Implementation notes: the returned <code>InputStream</code> uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     */
    @Override
    public RandomAccessOutputStream getRandomAccessOutputStream() throws IOException {
        return new LocalRandomAccessOutputStream(new RandomAccessFile(file, "rw").getChannel());
    }

    @Override
    public void delete() throws IOException {
        boolean ret = file.delete();

        if (!ret) {
            throw new IOException();
        }
    }

    @Override
    public AbstractFile[] ls() throws IOException {
        return ls((FilenameFilter) null);
    }

    @Override
    public void mkdir() throws IOException {
        if (!file.mkdir()) {
            throw new IOException();
        }
    }

    @Override
    public void renameTo(AbstractFile destFile) throws IOException, UnsupportedFileOperationException {
        // Throw an exception if the file cannot be renamed to the specified destination.
        // Fail in some situations where java.io.File#renameTo() doesn't.
        // Note that java.io.File#renameTo()'s implementation is system-dependant, so it's always a good idea to
        // perform all those checks even if some are not necessary on this or that platform.
        checkRenamePrerequisites(destFile, true, false);

        destFile = destFile.getTopAncestor();
        File destJavaIoFile = ((LocalFile) destFile).file;

        if (!file.renameTo(destJavaIoFile))
            throw new IOException();
    }

    @Override
    public long getFreeSpace() throws IOException {
        return file.getUsableSpace();
    }

    @Override
    public long getTotalSpace() throws IOException {
        return file.getTotalSpace();
    }

    // Unsupported file operations

    /**
     * Always throws {@link UnsupportedFileOperationException} when called.
     *
     * @throws UnsupportedFileOperationException,
     *             always
     */
    @Override
    @UnsupportedFileOperation
    public void copyRemotelyTo(AbstractFile destFile) throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.COPY_REMOTELY);
    }

    ////////////////////////
    // Overridden methods //
    ////////////////////////

    @Override
    public String getName() {
        if (isRoot()) {
            return "/";
        }

        return file.getName();
    }

    @Override
    public String getAbsolutePath() {
        // Append separator for root folders (C:\ , /) and for directories
        if (isRoot() || (isDirectory() && !absPath.endsWith(SEPARATOR))) {
            return absPath + SEPARATOR;
        }

        return absPath;
    }

    @Override
    public String getCanonicalPath() {
        // Note: canonical path must not be cached as its resolution can change over time, for instance
        // if a file 'Test' is renamed to 'test' in the same folder, its canonical path would still be 'Test'
        // if it was resolved prior to the renaming and thus be recognized as a symbolic link
        String canonicalPath;

        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            LOGGER.error("failed to retrieve canonical path of {}, returning {}", this, absPath);
            LOGGER.error("exception", e);
            return absPath;
        }

        if (isDirectory()) {
            canonicalPath = addTrailingSeparator(canonicalPath);
        }

        return canonicalPath;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public AbstractFile[] ls(FilenameFilter filenameFilter) throws IOException {
        File files[] = file.listFiles(filenameFilter == null ? null : new LocalFilenameFilter(filenameFilter));

        if (files == null) {
            throw new IOException();
        }

        int nbFiles = files.length;
        AbstractFile children[] = new AbstractFile[nbFiles];

        for (int i = 0; i < nbFiles; i++) {
            // Clone the FileURL of this file and set the child's path, this is more efficient than creating a new
            // FileURL instance from scratch.
            FileURL childURL = (FileURL) fileURL.clone();

            childURL.setPath(absPath + SEPARATOR + files[i].getName());

            // Retrieves an AbstractFile (LocalFile or AbstractArchiveFile) instance that's potentially already in
            // the cache, reuse this file as the file's parent, and the already-created java.io.File instance.
            children[i] = FileFactory.getFile(childURL, this, Collections.singletonMap("createdFile", files[i]));
        }

        return children;
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @Override
    public boolean canRead() {
        return file.canRead();
    }

    @Override
    public AbstractFile getRoot() {
        return super.getRoot();
    }

    @Override
    public boolean isRoot() {
        return super.isRoot();
    }

    /**
     * Overridden to return the local volume on which this file is located. The returned volume is one of the volumes
     * returned by {@link #getVolumes()}.
     */
    @Override
    public AbstractFile getVolume() {
        AbstractFile[] volumes = LocalFile.getVolumes();

        // Looks for the volume that best matches this file, i.e. the volume that is the deepest parent of this file.
        // If this file is itself a volume, return it.
        int bestDepth = -1;
        AbstractFile bestMatch = null;
        String thisPath = getAbsolutePath(true);

        for (AbstractFile volume : volumes) {
            String volumePath = volume.getAbsolutePath(true);

            if (thisPath.equals(volumePath)) {
                return this;
            } else if (thisPath.startsWith(volumePath)) {
                int depth = PathUtils.getDepth(volumePath, volume.getSeparator());
                if (depth > bestDepth) {
                    bestDepth = depth;
                    bestMatch = volume;
                }
            }
        }

        if (bestMatch != null) {
            return bestMatch;
        }

        // If no volume matched this file (shouldn't normally happen), return the root folder
        return getRoot();
    }

    ///////////////////
    // Inner classes //
    ///////////////////

    /**
     * LocalRandomAccessInputStream extends RandomAccessInputStream to provide random read access to a LocalFile. This
     * implementation uses a NIO <code>FileChannel</code> under the hood to benefit from
     * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted using
     * <code>Thread#interrupt()</code>.
     */
    public static class LocalRandomAccessInputStream extends RandomAccessInputStream {

        private final FileChannel channel;
        private final ByteBuffer bb;

        public LocalRandomAccessInputStream(FileChannel channel) {
            this.channel = channel;
            this.bb = BufferPool.getByteBuffer();
        }

        @Override
        public int read() throws IOException {
            synchronized (bb) {
                bb.position(0);
                bb.limit(1);

                int nbRead = channel.read(bb);
                if (nbRead <= 0)
                    return nbRead;

                return 0xFF & bb.get(0);
            }
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            synchronized (bb) {
                bb.position(0);
                bb.limit(Math.min(bb.capacity(), len));

                int nbRead = channel.read(bb);
                if (nbRead <= 0)
                    return nbRead;

                bb.position(0);
                bb.get(b, off, nbRead);

                return nbRead;
            }
        }

        @Override
        public void close() throws IOException {
            BufferPool.releaseByteBuffer(bb);
            channel.close();
        }

        public long getOffset() throws IOException {
            return channel.position();
        }

        public long getLength() throws IOException {
            return channel.size();
        }

        public void seek(long offset) throws IOException {
            channel.position(offset);
        }
    }

    /**
     * A replacement for <code>java.io.FileInputStream</code> that uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     *
     * <p>
     * This class simply delegates all its methods to a
     * {@link dev.barebones.commander.commons.file.protocol.local.LocalFile.LocalRandomAccessInputStream} instance. Therefore,
     * this class does not derive from {@link dev.barebones.commander.commons.io.RandomAccessInputStream}, preventing
     * random-access methods from being used.
     * </p>
     *
     */
    public static class LocalInputStream extends FilterInputStream {

        public LocalInputStream(FileChannel channel) {
            super(new LocalRandomAccessInputStream(channel));
        }
    }

    /**
     * A replacement for <code>java.io.FileOutputStream</code> that uses a NIO {@link FileChannel} under the hood to
     * benefit from <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted
     * using <code>Thread#interrupt()</code>.
     *
     * <p>
     * This class simply delegates all its methods to a
     * {@link dev.barebones.commander.commons.file.protocol.local.LocalFile.LocalRandomAccessOutputStream} instance. Therefore,
     * this class does not derive from {@link dev.barebones.commander.commons.io.RandomAccessOutputStream}, preventing
     * random-access methods from being used.
     * </p>
     *
     */
    public static class LocalOutputStream extends FilteredOutputStream {

        public LocalOutputStream(FileChannel channel) {
            super(new LocalRandomAccessOutputStream(channel));
        }
    }

    /**
     * LocalRandomAccessOutputStream extends RandomAccessOutputStream to provide random write access to a LocalFile.
     * This implementation uses a NIO <code>FileChannel</code> under the hood to benefit from
     * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O to be gracefully interrupted using
     * <code>Thread#interrupt()</code>.
     */
    public static class LocalRandomAccessOutputStream extends RandomAccessOutputStream {

        private final FileChannel channel;
        private final ByteBuffer bb;

        public LocalRandomAccessOutputStream(FileChannel channel) {
            this.channel = channel;
            this.bb = BufferPool.getByteBuffer();
        }

        @Override
        public void write(int i) throws IOException {
            synchronized (bb) {
                bb.position(0);
                bb.limit(1);

                bb.put((byte) i);
                bb.position(0);

                channel.write(bb);
            }
        }

        @Override
        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            int nbToWrite;
            synchronized (bb) {
                do {
                    bb.position(0);
                    nbToWrite = Math.min(bb.capacity(), len);
                    bb.limit(nbToWrite);

                    bb.put(b, off, nbToWrite);
                    bb.position(0);

                    nbToWrite = channel.write(bb);

                    len -= nbToWrite;
                    off += nbToWrite;
                } while (len > 0);
            }
        }

        @Override
        public void setLength(long newLength) throws IOException {
            long currentLength = getLength();

            if (newLength == currentLength)
                return;

            long currentPos = channel.position();

            if (newLength < currentLength) {
                // Truncate the file and position the offset to the new EOF if it was beyond
                channel.truncate(newLength);
                if (currentPos > newLength) {
                    channel.position(newLength);
                }
            } else {
                // Expand the file by positionning the offset at the new EOF and writing a byte, and reposition the
                // offset to where it was
                channel.position(newLength - 1); // Note: newLength cannot be 0
                write(0);
                channel.position(currentPos);
            }

        }

        @Override
        public void close() throws IOException {
            BufferPool.releaseByteBuffer(bb);
            channel.close();
        }

        public long getOffset() throws IOException {
            return channel.position();
        }

        public long getLength() throws IOException {
            return channel.size();
        }

        public void seek(long offset) throws IOException {
            channel.position(offset);
        }
    }

    /**
     * A Permissions implementation for LocalFile.
     */
    private static class LocalFilePermissions extends IndividualPermissionBits implements FilePermissions {

        private java.io.File file;

        // Permissions are limited to the user access type.

        private final static PermissionBits MASK = new GroupedPermissionBits(448); // rwx------ (700 octal)

        private LocalFilePermissions(java.io.File file) {
            this.file = file;
        }

        public boolean getBitValue(PermissionAccess access, PermissionType type) {
            // Only the 'user' permissions are supported
            if (access != PermissionAccess.USER) {
                return false;
            }

            switch (type) {
            case READ:
                return file.canRead();
            case WRITE:
                return file.canWrite();
            case EXECUTE:
                return file.canExecute();
            default:
                return false;
            }
        }

        /**
         * Overridden for performance reasons.
         */
        @Override
        public int getIntValue() {
            int userPerms = 0;

            if (getBitValue(PermissionAccess.USER, PermissionType.READ)) {
                userPerms |= PermissionType.READ.toInt();
            }

            if (getBitValue(PermissionAccess.USER, PermissionType.WRITE)) {
                userPerms |= PermissionType.WRITE.toInt();
            }

            if (getBitValue(PermissionAccess.USER, PermissionType.EXECUTE)) {
                userPerms |= PermissionType.EXECUTE.toInt();
            }

            return userPerms << 6;
        }

        public PermissionBits getMask() {
            return MASK;
        }
    }

    /**
     * Turns a {@link FilenameFilter} into a {@link java.io.FilenameFilter}.
     */
    private static class LocalFilenameFilter implements java.io.FilenameFilter {

        private FilenameFilter filter;

        private LocalFilenameFilter(FilenameFilter filter) {
            this.filter = filter;
        }

        ///////////////////////////////////////////
        // java.io.FilenameFilter implementation //
        ///////////////////////////////////////////

        public boolean accept(File dir, String name) {
            return filter.accept(name);
        }
    }

}
