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


package dev.barebones.commander.commons.file;

import java.net.MalformedURLException;
import java.net.URLDecoder;

import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import dev.barebones.commander.commons.file.protocol.local.LocalFile;

/**
 * This class provides a default {@link SchemeParser} implementation. Certain scheme-specific features of the parser
 * can be turned on or off in the constructor, allowing this parser to be used with most schemes.
 *
     * <p>This parser can not only parse URLs but also local absolute paths. Upon parsing, these paths are
 * turned into equivalent, fully qualified URLs.</p>
 *
 * <h3>Local paths</h3>
 * <p>
     * Local absolute paths are turned into corresponding 'file' URLs. macOS/Linux-style absolute paths are supported.
 * </p>
 *
 * @see PathCanonizer
 * @author Maxence Bernard
 */
public class DefaultSchemeParser implements SchemeParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSchemeParser.class);

    /** True if query should be parsed and not considered as part of the path */
    protected boolean parseQuery;

    /** <code>PathCanonizer</code> instance to be used for canonizing the path part */
    protected PathCanonizer pathCanonizer;


    /**
     * Creates a DefaultSchemeParser with a {@link DefaultPathCanonizer} that uses the operating system's default 
     * path separator as the path separator and no tilde replacement, and query parsing disabled.
     */
    public DefaultSchemeParser() {
        this(false);
    }

    /**
     * Creates a DefaultSchemeParser with a {@link DefaultPathCanonizer} that uses that uses the operating system's
	 * default path separator as the path separator and no tilde replacement.
     * If <code>parseQuery</code> is <code>true</code>, any query part (delimited by '?') will be parsed as such,
     * or considered as part of the path otherwise.
     *
     * @param parseQuery <code>true</code>, any query part (delimited by '?') will be parsed as such, or considered
     * as part of the path otherwise
     */
    public DefaultSchemeParser(boolean parseQuery) {
        this(new DefaultPathCanonizer(System.getProperty("file.separator"), null), parseQuery);
    }

    /**
     * Creates a DefaultSchemeParser using the specified {@link PathCanonizer} for canonizing the path part.
     * If <code>parseQuery</code> is <code>true</code>, any query part (delimited by '?') will be parsed as such,
     * or considered as part of the path otherwise.
     *
     * @param pathCanonizer <code>PathCanonizer</code> instance to be used for canonizing the path part
     * @param parseQuery <code>true</code>, any query part (delimited by '?') will be parsed as such, or considered
     * as part of the path otherwise
     */
    public DefaultSchemeParser(PathCanonizer pathCanonizer, boolean parseQuery) {
        this.parseQuery = parseQuery;
        this.pathCanonizer = pathCanonizer;
    }

    /**
     * Handles the parsing of the given local file URL.
     *
     * @param url the URL to parse
     * @param fileURL the FileURL instance in which to set the different parsed parts
     */
    private void handleLocalFilePath(String url, FileURL fileURL) {
        SchemeHandler handler = FileURL.getRegisteredHandler(LocalFile.SCHEMA);
        SchemeParser parser = handler.getParser();

        fileURL.setHandler(handler);
        fileURL.setScheme(LocalFile.SCHEMA);
        fileURL.setHost(FileURL.LOCALHOST);
        fileURL.setPath((parser instanceof DefaultSchemeParser?((DefaultSchemeParser)parser).getPathCanonizer():pathCanonizer).canonize(url));
    }

    /**
     * Returns the {@link PathCanonizer} instance that is used by this {@link DefaultSchemeParser}.
     *
     * @return the {@link PathCanonizer} instance that is used by this {@link DefaultSchemeParser}
     */
    public PathCanonizer getPathCanonizer() {
        return pathCanonizer;
    }



    /////////////////////////////////
    // SchemeParser implementation //
    /////////////////////////////////

    public void parse(String url, FileURL fileURL) throws MalformedURLException {
        // The general form of a URI is:

        //      foo://example.com:8042/over/there?name=ferret#nose
        //      \_/   \______________/\_________/ \_________/ \__/
        //       |           |            |            |        |
        //    scheme     authority       path        query   fragment
        //       |   _____________________|__
        //      / \ /                        \
        //      urn:example:animal:ferret:nose


        // See http://labs.apache.org/webarch/uri/rfc/rfc3986.html for full specs

        try {
            int schemeDelimPos = url.indexOf("://");
            int urlLen = url.length();

            // If the given url contains no scheme, consider that it is a local path and transform it into a file:// URL
            if(schemeDelimPos==-1) {
                // Treat the URL as local file path if it starts with:
                // - '/' for Unix-style paths
                // - a ~ character (refers to the user home folder)
                if (url.startsWith("/") || url.startsWith("~/") || url.equals("~")) {
                    handleLocalFilePath(url, fileURL);

                    // All done, return
                    return;
                }
                // This doesn't look like a valid path, throw an MalformedURLException
                else {
                    throw new MalformedURLException("Path not absolute or malformed: "+url);
                }
            }

            // Start URL parsing

            String scheme = url.substring(0, schemeDelimPos);
            fileURL.setScheme(scheme);
            // Advance string index
            int pos = schemeDelimPos+3;

            int separatorPos = url.indexOf('/', pos);

            // The question mark character (if any) marks the beginning of the query part, only if it should be parsed.
            int questionMarkPos = parseQuery?url.indexOf('?', pos):-1;
            int hostEndPos;         // Contains the position of the beginning of the path/query part
            if(separatorPos!=-1)    // Separator is necessarily before question mark
                hostEndPos = separatorPos;
            else if(questionMarkPos !=-1)
                hostEndPos = questionMarkPos;
            else
                hostEndPos = urlLen;

            // The authority part is the one between scheme:// and the path/query. It includes the user information
            // (login/password), host and port. 
            String authority = url.substring(pos, hostEndPos);
            pos = 0;

            // Parse login and password (if specified).
            // They may contain non-URL safe characters that are decoded here, and re-encoded by FileURL#toString.
            int atPos = authority.lastIndexOf('@');
            int colonPos;
            // Filenames may contain @ chars, so atPos must be lower than next separator's position (if any)
            if(atPos!=-1 && (separatorPos==-1 || atPos<separatorPos)) {
                colonPos = authority.indexOf(':');
                String login = URLDecoder.decode(authority.substring(0, colonPos==-1?atPos:colonPos), "UTF-8");
                String password;
                if(colonPos!=-1)
                    password = URLDecoder.decode(authority.substring(colonPos+1, atPos), "UTF-8");
                else
                    password = null;

                if(!"".equals(login) || !(password==null || "".equals(password)))
                    fileURL.setCredentials(new Credentials(login, password));

                // Advance string index
                pos = atPos+1;
            }

            // Parse host and port (if specified)
            colonPos = authority.indexOf(':', pos);

            String host;
            if(colonPos!=-1) {
                host = authority.substring(pos, colonPos);
                String portString = authority.substring(colonPos+1);
                if(!portString.equals("")) {        // Tolerate an empty port part (e.g. http://mucommander.com:/)
                    try {
                        fileURL.setPort(Integer.parseInt(portString));
                    }
                    catch(NumberFormatException e) {
                        throw new MalformedURLException("URL contains an invalid port");
                    }
                }
            }
            else {
                host = authority.substring(pos);
            }

            if(host.equals(""))
                host = null;

            fileURL.setHost(host);

            // Parse path part excluding query part
            pos = hostEndPos;
            String path = url.substring(pos, questionMarkPos==-1?urlLen:questionMarkPos);

            // Empty path means '/'
            if (path.equals("")) {
                LOGGER.info("Warning: path should not be empty, url={}", url);
                path = "/";
            }

            // Canonize path: factor out '.' and '..' and replace '~' by the replacement string (if any)
            fileURL.setPath(pathCanonizer.canonize(path));

            // Parse query part (if any)
            if(questionMarkPos!=-1)
                fileURL.setQuery(url.substring(questionMarkPos+1));     // Do not include the question mark
        }
        catch(MalformedURLException e) {
            throw e;
        }
        catch(Exception e2) {
            LOGGER.info("Unexpected exception in FileURL() with "+url, e2);

            throw new MalformedURLException();
        }
    }
}
