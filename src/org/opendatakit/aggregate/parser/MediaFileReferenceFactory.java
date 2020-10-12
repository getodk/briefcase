package org.opendatakit.aggregate.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceFactory;

/**
 * ReferenceFactory implementation that resolves any jr:// URI to the specified media folder. Most methods are
 * unimplemented and throw UnsupportedOperationException if used.
 */
@SuppressWarnings("checkstyle:ParameterName")
public class MediaFileReferenceFactory implements ReferenceFactory {
  private final File mediaDirectory;

  public MediaFileReferenceFactory(File mediaDirectory) {
    this.mediaDirectory = mediaDirectory;
  }

  @Override
  public boolean derives(String URI) {
    return true;
  }

  @Override
  public Reference derive(String URI) throws InvalidReferenceException {
    return new Reference() {
      @Override
      public String getLocalURI() {
        return mediaDirectory.getAbsolutePath() + URI.substring(URI.lastIndexOf('/'));
      }

      @Override
      public boolean doesBinaryExist() throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public InputStream getStream() throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getURI() {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isReadOnly() {
        return false;
      }

      @Override
      public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public void remove() throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public Reference[] probeAlternativeReferences() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public Reference derive(String URI, String context) throws InvalidReferenceException {
    throw new UnsupportedOperationException();
  }
}
