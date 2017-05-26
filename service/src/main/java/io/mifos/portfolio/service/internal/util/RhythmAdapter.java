/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.portfolio.service.internal.util;

import io.mifos.core.lang.ApplicationName;
import io.mifos.portfolio.service.ServiceConstants;
import io.mifos.rhythm.api.v1.client.RhythmManager;
import io.mifos.rhythm.api.v1.domain.Beat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

/**
 * @author Myrle Krantz
 */
@Component
public class RhythmAdapter {
  final private RhythmManager rhythmManager;
  final private ApplicationName applicationName;
  final private Logger logger;
  @Autowired
  public RhythmAdapter(@SuppressWarnings("SpringJavaAutowiringInspection") final RhythmManager rhythmManager,
                       final ApplicationName applicationName,
                       final @Qualifier(ServiceConstants.LOGGER_NAME)Logger logger) {
    this.rhythmManager = rhythmManager;
    this.applicationName = applicationName;
    this.logger = logger;
  }

  public void request24Beats() {
    Stream.iterate(0, x -> x+1).limit(24)
            .map(RhythmAdapter::defineBeat)
            .forEach(this::createBeat);
  }

  private static Beat defineBeat(final int alignmentHour) {
    final Beat beat = new Beat();
    beat.setAlignmentHour(alignmentHour);
    beat.setIdentifier("alignment" + alignmentHour);
    return beat;
  }

  private void createBeat(final Beat beat) {
    try {
      rhythmManager.createBeat(applicationName.toString(), beat);
    }
    catch (final Exception e) {
      logger.error("Creating interest calculation beat {} failed with exception e.", beat, e);
    }
  }
}