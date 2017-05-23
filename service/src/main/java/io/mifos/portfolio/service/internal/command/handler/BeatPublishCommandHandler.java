package io.mifos.portfolio.service.internal.command.handler;

import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ApplicationName;
import io.mifos.rhythm.spi.v1.domain.BeatPublish;
import io.mifos.rhythm.spi.v1.events.BeatPublishEvent;
import io.mifos.rhythm.spi.v1.events.EventConstants;
import io.mifos.portfolio.service.internal.command.CreateBeatPublishCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
@Aggregate
public class BeatPublishCommandHandler {

  private final ApplicationName applicationName;

  @Autowired
  public BeatPublishCommandHandler(final ApplicationName applicationName) {
    this.applicationName = applicationName;
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_PUBLISHEDBEAT)
  public BeatPublishEvent process(final CreateBeatPublishCommand createBeatPublishCommand) {
    final BeatPublish instance = createBeatPublishCommand.getInstance();
    return new BeatPublishEvent(applicationName.toString(), instance.getIdentifier(), instance.getForTime());
  }
}
