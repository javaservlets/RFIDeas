/*
Author : IntimeTec Visionsoft Private Limited PLUS ForgeRock AS.
Creation Date : 16 Feb,2017
(Updated Date: 16 May, 2019 robbie.jones@forgerock.com)
This sample application uses "jna" which does not
comes by default along with jdk. You have to download
"jna.jar" and reference jna.jar in your project's CLASSPATH.

This example merely a proof of concept that rfideas's
SDK(which is a C/C++ library) can be used in Java.
Developers are free to use any other binding framework of
their choice.
*/

/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */

package com.rfideas.forgerock;

import com.sun.jna.Library;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
//import static com.rfideas.forgerock.BadgeTap.*;

import static com.sun.jna.Native.loadLibrary;

public class ReaderComm {
    public interface Reader extends Library {
        /*
        Helper of  usbconnect, Will restrict
        the device search to a particular category
        for example.
        Device Type could be one of following
        0 : To search only USB devices
        1 :  To Search Serial RS-232 only.
        -1 : Both USB and serial devices.
        This function will return true in case of
        success false otherwise.
        */
        public short SetDevTypeSrch(short iSrchType);

        /*
        return true in case success, false otherwise.
        Will open connection to all rfidea's readers/devices
        in onces.
        Individual device can be accessed by using setActiveDev first
        then call other functions.
        */
        public int usbConnect();

        /*
        Will return true in case success, false otherwise.
        As per API documentation USBDisconnect always returns
        true.
        It will close the handle to all rfidea's devices, should be
        called during clean up.
        */
        public int USBDisconnect();

        //Will return the LUID of active device/Reader.
        public int GetLUID();

        //Will Return the total number of connected rfidea's readers.
        public short GetDevCnt();

        //Will return true if able to set active device to given device, false otherwise
        public short SetActDev(short iNdx);

        //Will return the part number of active device and none in case of failure.
        public String getPartNumberString();

        /*
        Will return None in case of error other wise will return
        the VID PID and product name in following format
        <VID>:<PID> <product name>
        */
        public String GetVidPidVendorName();

        /*
        Will return a tuple consisting number of bits read and actual data.
        Minimum 8 byte of data will be returned.
        */
        public short GetActiveID32(byte[] pBuf, short wBufMaxSz);

        /*
        Will return the SDK version in following format
        <Major>.<Minor>.<Dev>.
        In case of error will return None
        */
        public short GetLibVersion(int[] piVerMaj, int[] piVerMin, int[] piVerDev);
    }

    static Reader lib;

    //
    // this fires when '-enroll (username)' is passed as an arg, and when a badge is tapped against a reader, it writes that value to the user's "sunIdentityMSISDNNumber" attribute
    //

    static String badgeID = "";
    static BadgeTap tap; //scans for a tap and writes the hex value to a Q

    public static void enroll(String user) {
        final long timeInterval = 2000;
        Runnable runnable = new Runnable() {
            public void run() {
                while (badgeID.equals("")) { //repeat until getActiveID returns a value
                    System.out.println("To enroll user {" + user + "} please tap the badge to the reader now " + badgeID);
                    badgeID = getActiveId32(lib); // their OOTB SDK call to their HW
                    try {
                        Thread.sleep(timeInterval);
                    } catch (InterruptedException e) {
                        System.out.println("er: "
                                + e);
                        e.printStackTrace();
                    }
                }
                System.out.println("\n >> Enrolling " + user + " with that badge info");
                updateFR(user, badgeID); //write the attribute
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public static void updateFR(String usr, String badgeID) {
        String toke = EnrollUser.getToken();
        EnrollUser.updateAttribute(usr, badgeID, toke);
    }

    //
    // this fires when '-mfa (username)' is passed as an arg, and later a badge is tapped against a reader
    // (update 5.16.19) '-headless' will write to a static key called 'headless' for 'touch and go' (ie, no username is provided)

    public static void scanBadge(String usr) {
        tap = new BadgeTap();
        final long timeInterval = 1000;
        Runnable runnable = new Runnable() {
            public void run() {
                while (badgeID.equals("")) { //repeat until getActiveID returns a value
                    System.out.println("to verify please tap your badge to the reader now " + badgeID);
                    badgeID = getActiveId32(lib);
                    try {
                        Thread.sleep(timeInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("\n >> verifying badge " +  badgeID);
                tap.update(usr, (badgeID + " ^ " + getTime()));
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    static private String getTime() { // we'll write a timestamp along with the hex value read
        Date date = null;
        String formattedDate = null;
        try {
            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            date = new Date(stamp.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            formattedDate = sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formattedDate;
    }

    //
    // 99% of the below is the OOTB rfIdea SDK code (the other 1% has 'rj' comments next to them)
    //

    public static void main(String args[]) throws IOException {
        String lib_home = "";
        String lib_name = "";

        if (System.getProperty("os.arch").equals("amd64")) {
            //checking whether it is 64 bit process
            lib_home = "../../lib/64";
        } else {
            //it is 32 bit process
            lib_home = "../../lib/32";
        }
        if (System.getProperty("os.name").startsWith("Mac OS X")) {
            //checking whether the OS is Mac OS X so that we change lib_home path
            lib_home = "lib"; //rj was ../../lib but that didn't match up with what I have in this distro
            //Getting absolute path for Mac OS X
            Path path = Paths.get(lib_home);
            lib_home = path.toFile().getCanonicalPath();
        }
        if (System.getenv("RFI_DLL_HOME") != null) {
            lib_home = System.getenv("RFI_DLL_HOME");
        }
        if (System.getProperty("os.name").startsWith("Mac OS X")) {
            //checking whether the OS is Mac OS X
            lib_name = "libpcProxAPI.dylib";
        } else if (System.getProperty("os.name").startsWith("Windows")) {
            //checking whether the OS is Windows
            lib_name = "pcProxAPI.dll";
        } else {
            //OS is Linux
            lib = (Reader) loadLibrary(lib_home + "/libhidapi-hidraw.so", Reader.class);
            lib_name = "libpcProxAPI.so";
        }

        lib = (Reader) loadLibrary(lib_home + "/" + lib_name, Reader.class);

        if (args.length == 0) {
            showHelpText();
        } else if (args[0].equals("--enumerate")) {
            listAllConnectedRFIDevice(lib);
        } else if (args[0].equals("--sdk-version")) {
            String sdk_version = getLibVersion(lib);
            System.out.println("\nSDK Version : " + sdk_version);
        } else if (args[0].equals("--getid")) {
            getActiveId32(lib);
        } else if (args[0].equals("--help")) {
            showHelpText();


        } else if (args[0].contains("--enroll")) { //rj added these two args (both require a username to be passed in as well)
            if (args.length == 1) // ie, if arg[1] == null
                showHelpText();
            else
                enroll(args[1]);
        } else if (args[0].contains("--mfa")) {
            if (args.length == 1) // ie, if arg[1] == null
                showHelpText();
            else
                scanBadge(args[1]); // name of user is passed in
        } else if (args[0].contains("--headless")) {
            scanBadge("headless");
        }
    }

    public static void listAllConnectedRFIDevice(Reader lib) {
        short PRXDEVTYP_USB = 0;
        lib.SetDevTypeSrch(PRXDEVTYP_USB);
        if (lib.usbConnect() == 0) {
            System.out.println("\nReader not connected");
            return;
        }
        short getDevCnt = lib.GetDevCnt();
        System.out.println("\nPartNumber" + "\t\t" + "Vid:Pid" + "\t\t\t\t" + "LUID\n");
        for (short i = 0; i < getDevCnt; i++) {
            lib.SetActDev(i);
			/*
			It is a hack.
			If the LUID value of reader is greater than 32767 then,
			GetLUID() will return negative value, therefore, it is required
			to perform 'and' operation between obtained value and 0x0000FFFF
			so that, we always get correct LUID value.
			*/
            int luid = lib.GetLUID() & 0x0000FFFF;
            System.out.println(lib.getPartNumberString() + "\t\t" + lib.GetVidPidVendorName() + "\t\t" + luid);
        }
        lib.USBDisconnect();
    }

    public static String getLibVersion(Reader lib) {
        int major[] = new int[1];
        int minor[] = new int[1];
        int build[] = new int[1];
        lib.GetLibVersion(major, minor, build);
        String libVersion = major[0] + "." + minor[0] + "." + build[0];
        return libVersion;
    }

    public static String getActiveId32(Reader lib) { //rj used to b 'void'
        StringBuilder sb = new StringBuilder();
        short PRXDEVTYP_USB = 0;
        lib.SetDevTypeSrch(PRXDEVTYP_USB);
        if (lib.usbConnect() == 0) {
            System.out.println("\nReader not connected");
            return "";
        }

        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        short wBufMaxSz = 32;
        byte buf[] = new byte[wBufMaxSz];
        short bits = lib.GetActiveID32(buf, wBufMaxSz);

        if (bits == 0) {
//            String errorMessage = "\nNo id found, Please put card on the reader and " +
//                    "make sure it must be configured with the card placed on it.";
//            System.out.println(errorMessage);
        } else {
            int bytes_to_read = (bits + 7) / 8;
            if (bytes_to_read < 8) {
                bytes_to_read = 8;
            }
            System.out.print("\nBadge read had these " + bits + " Bits : ");
            for (int i = bytes_to_read - 1; i >= 0; i--) {
                System.out.printf("%02X ", buf[i]);
                sb.append(String.format("%02X ", buf[i]));
            }
        }
        lib.USBDisconnect();
        return sb.toString();
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void showHelpText() {
        System.out.println("\nUsage: readercomm [options]\n");
        System.out.println("--enroll {name} \t\t update {username's} profile with badge ID "); //rj added
        //System.out.println("--mfa {name} \t\t authenticate via a badge tap "); //rj added
        System.out.println("--headless \t\t authenticate via 'tap and go' (ie, no username required upfront)"); //rj added
        System.out.println("--enumerate\t\t list all the connected rfidea's readers");
        System.out.println("--sdk-version\t\t give the sdk version");
        System.out.println("--help\t\t         print this help");
        System.out.println("--getid\t\t         give raw data of card which being read");
    }

    public static void log(String str) {
        System.out.println(str);
    }
}
