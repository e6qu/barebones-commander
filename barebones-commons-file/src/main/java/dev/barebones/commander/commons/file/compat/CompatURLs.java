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

package dev.barebones.commander.commons.file.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;

public final class CompatURLs {

    private CompatURLs() {
    }

    public static URL create(String spec, URLStreamHandler handler) throws MalformedURLException {
        try {
            return URL.of(URI.create(spec), handler);
        }
        catch(IllegalArgumentException e) {
            return createWithLegacyHandlerOverride(spec, handler, e);
        }
    }

    private static URL createWithLegacyHandlerOverride(String spec, URLStreamHandler handler, IllegalArgumentException cause) throws MalformedURLException {
        try {
            Constructor<URL> constructor = URL.class.getConstructor(URL.class, String.class, URLStreamHandler.class);
            return constructor.newInstance(null, spec, handler);
        }
        catch(InvocationTargetException e) {
            Throwable targetException = e.getCause();
            if(targetException instanceof MalformedURLException)
                throw (MalformedURLException)targetException;

            MalformedURLException malformedURLException = new MalformedURLException(spec);
            malformedURLException.initCause(targetException);
            throw malformedURLException;
        }
        catch(ReflectiveOperationException e) {
            MalformedURLException malformedURLException = new MalformedURLException(spec);
            malformedURLException.initCause(cause);
            throw malformedURLException;
        }
    }
}
