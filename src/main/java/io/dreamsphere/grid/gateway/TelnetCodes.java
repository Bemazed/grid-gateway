/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

class TelnetCodes {
    final static byte OPT_SUPPRESS_GO_AHEAD = 3;
    final static byte OPT_STATUS = 5;
    final static byte OPT_ECHO = 1;
    final static byte OPT_TIMING_MARK = 6;
    final static byte OPT_TERMINAL_TYPE = 24;
    final static byte OPT_WINDOW_SIZE = 31;
    final static byte OPT_TERMINAL_SPEED = 32;
    final static byte OPT_REMOTE_FLOW_CONTROL = 33;
    final static byte OPT_LINEMODE = 34;
    final static byte OPT_ENVIRONMENT_VARIABLES = 36;

    final static byte CMD_SE = (byte) 240;
    final static byte CMD_NOP = (byte) 241;
    final static byte CMD_DM = (byte) 242;
    final static byte CMD_BRK = (byte) 243;
    final static byte CMD_IP = (byte) 244;
    final static byte CMD_AO = (byte) 245;
    final static byte CMD_AYT = (byte) 246;
    final static byte CMD_EC = (byte) 247;
    final static byte CMD_EL = (byte) 248;
    final static byte CMD_GA = (byte) 249;
    final static byte CMD_SB = (byte) 250;
    final static byte CMD_WILL = (byte) 251;
    final static byte CMD_WONT = (byte) 252;
    final static byte CMD_DO = (byte) 253;
    final static byte CMD_DONT = (byte) 254;
    final static byte CMD_IAC = (byte) 255;
}
