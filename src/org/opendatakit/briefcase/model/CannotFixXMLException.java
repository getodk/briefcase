/*
 * Copyright (C) 2012-13 Dobility, Inc.
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

package org.opendatakit.briefcase.model;

/**
 * CTOSurvey contribution to address issues with Android 4.3 systems not
 * properly flushing OpenSSL CipherStreams.
 *
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 28/9/2013
 * Time: 10:00 μμ
 */
public class CannotFixXMLException extends Exception {

    public CannotFixXMLException(String message) {
        super(message);
    }

    public CannotFixXMLException(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotFixXMLException(Throwable cause) {
        super(cause);
    }
}
