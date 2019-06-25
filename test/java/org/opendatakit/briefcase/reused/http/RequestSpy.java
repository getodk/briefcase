package org.opendatakit.briefcase.reused.http;

import java.io.InputStream;
import java.util.Scanner;

public class RequestSpy<T> {
  Request<T> request;
  boolean called = false;
  private String body;

  public void track(Request<T> request) {
    this.request = request;
    called = true;
  }

  public String readBody() {
    if (body == null)
      body = read(request.getBody());
    return body;
  }

  static String read(InputStream is) {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
