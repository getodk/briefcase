/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.operations.export;

import static org.opendatakit.briefcase.reused.model.submission.SubmissionParser.parseEncryptedSubmission;
import static org.opendatakit.briefcase.reused.model.submission.SubmissionParser.parsePlainSubmission;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.model.form.FormMetadata;
import org.opendatakit.briefcase.reused.model.submission.Submission;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;

class ExportTools {
  static Stream<Submission> getSubmissions(FormMetadata formMetadata, ExportConfiguration configuration, List<SubmissionMetadata> submissionMetadataList, BiConsumer<SubmissionMetadata, String> onParsingError) {
    return submissionMetadataList.parallelStream()
        // Parse the submission and leave only those OK to be exported
        .map(submissionMetadata -> formMetadata.isEncrypted()
            ? parseEncryptedSubmission(submissionMetadata, configuration.getPrivateKey().orElseThrow(BriefcaseException::new), onParsingError)
            : parsePlainSubmission(submissionMetadata, onParsingError)
        )
        .filter(Optional::isPresent)
        .map(Optional::get);
  }
}
