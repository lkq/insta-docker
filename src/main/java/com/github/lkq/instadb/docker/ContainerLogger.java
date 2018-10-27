package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerLogger extends LogContainerResultCallback {

    private static Logger logger = LoggerFactory.getLogger(ContainerLogger.class);

    @Override
    public void onNext(Frame frame) {
        if (frame.getPayload().length > 0) {
            logger.info(new String(frame.getPayload(), 0, frame.getPayload().length - 1).trim());
        }
    }
}
