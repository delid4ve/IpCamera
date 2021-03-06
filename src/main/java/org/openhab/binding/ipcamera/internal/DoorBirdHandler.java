/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.ipcamera.internal;

import static org.openhab.binding.ipcamera.IpCameraBindingConstants.*;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ipcamera.handler.IpCameraHandler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

/**
 * The {@link DoorBirdHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Matthew Skinner - Initial contribution
 */

@NonNullByDefault
public class DoorBirdHandler extends ChannelDuplexHandler {
    IpCameraHandler ipCameraHandler;

    public DoorBirdHandler(ThingHandler handler) {
        ipCameraHandler = (IpCameraHandler) handler;
    }

    // This handles the incoming http replies back from the camera.
    @Override
    public void channelRead(@Nullable ChannelHandlerContext ctx, @Nullable Object msg) throws Exception {
        String content = null;
        try {
            content = msg.toString();
            if (!content.isEmpty()) {
                ipCameraHandler.logger.trace("HTTP Result back from camera is \t:{}:", content);
            } else {
                return;
            }
            if (content.contains("doorbell:H")) {
                ipCameraHandler.setChannelState(CHANNEL_DOORBELL, OnOffType.valueOf("ON"));
            }
            if (content.contains("doorbell:L")) {
                ipCameraHandler.setChannelState(CHANNEL_DOORBELL, OnOffType.valueOf("OFF"));
            }
            if (content.contains("motionsensor:L")) {
                ipCameraHandler.noMotionDetected(CHANNEL_MOTION_ALARM);
            }
            if (content.contains("motionsensor:H")) {
                ipCameraHandler.motionDetected(CHANNEL_MOTION_ALARM);
            }

        } finally {
            ReferenceCountUtil.release(msg);
            content = null;
        }
    }

    // This handles the commands that come from the Openhab event bus.
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command.toString() == "REFRESH") {
            return;
        } // end of "REFRESH"
        switch (channelUID.getId()) {
            case CHANNEL_ACTIVATE_ALARM_OUTPUT:
                if ("ON".equals(command.toString())) {
                    ipCameraHandler.sendHttpGET("/bha-api/open-door.cgi");
                }
                return;
            case CHANNEL_ACTIVATE_ALARM_OUTPUT2:
                if ("ON".equals(command.toString())) {
                    ipCameraHandler.sendHttpGET("/bha-api/open-door.cgi?r=2");
                }
                return;
            case CHANNEL_EXTERNAL_LIGHT:
                if ("ON".equals(command.toString())) {
                    ipCameraHandler.sendHttpGET("/bha-api/light-on.cgi");
                }
                return;
        }
    }

    // If a camera does not need to poll a request as often as snapshots, it can be
    // added here. Binding steps through the list.
    public ArrayList<String> getLowPriorityRequests() {
        ArrayList<String> lowPriorityRequests = new ArrayList<String>(1);
        return lowPriorityRequests;
    }
}