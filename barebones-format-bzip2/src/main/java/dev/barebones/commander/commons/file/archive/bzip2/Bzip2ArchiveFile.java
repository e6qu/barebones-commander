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


package dev.barebones.commander.commons.file.archive.bzip2;

import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.file.archive.AbstractROArchiveFile;
import dev.barebones.commander.commons.file.archive.ArchiveEntry;
import dev.barebones.commander.commons.file.archive.ArchiveEntryIterator;
import dev.barebones.commander.commons.file.archive.SingleArchiveEntryIterator;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Bzip2ArchiveFile provides read-only access to archives in the Bzip2 format.
 *
 * <p>The actual decompression work is performed by the <code>Apache Ant</code> library under the terms of the
 * Apache Software License.</p>
 *
 * @see dev.barebones.commander.commons.file.archive.bzip2.Bzip2FormatProvider
 * @author Maxence Bernard
 */
public class Bzip2ArchiveFile extends AbstractROArchiveFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bzip2ArchiveFile.class);

    /**
     * Creates a BzipArchiveFile on top of the given file.
     *
     * @param file the underlying file to wrap this archive file around
     */
    public Bzip2ArchiveFile(AbstractFile file) {
        super(file);
    }


    ////////////////////////////////////////
    // AbstractArchiveFile implementation //
    ////////////////////////////////////////

    @Override
    public ArchiveEntryIterator getEntryIterator() throws IOException {
        String extension = getCustomExtension() != null ? getCustomExtension() : getExtension();
        String name = getName();

        if (extension != null) {
            // Remove the 'bz2' or 'tbz2' extension from the entry's name
            extension = extension.toLowerCase();
            int extensionIndex = name.toLowerCase().lastIndexOf("." + extension);

            if (extensionIndex > -1)
                name = name.substring(0, extensionIndex);

            if (extension.equals("tbz2") || extension.equals("tar.bz2"))
                name += ".tar";
        }

        return new SingleArchiveEntryIterator(new ArchiveEntry("/"+name, false, getDate(), -1, true));
    }

    @Override
    public InputStream getEntryInputStream(ArchiveEntry entry, ArchiveEntryIterator entryIterator) throws IOException {
        try {
            InputStream in = getInputStream();

            return new BZip2CompressorInputStream(in);
        }
        catch(Exception e) {
            LOGGER.info("Exception caught while creating BZip2CompressorInputStream, throwing IOException", e);

            throw new IOException();
        }
    }
}
