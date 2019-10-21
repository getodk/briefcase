/*
 * Copyright (C) 2019 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.reused.http;

import java.io.InputStream;
import java.util.Objects;

public class MultipartMessage {
  private final String name;
  private final String contentType;
  private final String attachmentName;
  private final InputStream body;

  MultipartMessage(String name, String contentType, String attachmentName, InputStream body) {
    this.name = name;
    this.contentType = contentType;
    this.attachmentName = attachmentName;
    this.body = body;
  }

  public String getName() {
	return name;
  }

  public String getContentType() {
	return contentType;
  }

  public String getAttachmentName() {
	return attachmentName;
  }

  public InputStream getBody() {
	return body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultipartMessage that = (MultipartMessage) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(contentType, that.contentType) &&
        Objects.equals(attachmentName, that.attachmentName) &&
        Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, contentType, attachmentName, body);
  }

  @Override
  public String toString() {
    return String.format(
        "MultipartMessage(%s, %s, %s)",
        name,
        contentType,
        attachmentName
    );
  }
}
