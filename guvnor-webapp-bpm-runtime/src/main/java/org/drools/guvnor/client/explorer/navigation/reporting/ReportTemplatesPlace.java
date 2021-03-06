/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.client.explorer.navigation.reporting;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class ReportTemplatesPlace extends Place {

    @Override
    public boolean equals(Object o) {
        if (o instanceof ReportTemplatesPlace) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static class Tokenizer implements PlaceTokenizer<ReportTemplatesPlace> {

        public ReportTemplatesPlace getPlace(String token) {
            return new ReportTemplatesPlace();
        }

        public String getToken(ReportTemplatesPlace place) {
            return "REPORT_TEMPLATES_PLACE";
        }
    }
}
