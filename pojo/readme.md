
This is a simple stand-alone java class for the ForgeRock integration with the smart badge reader (model "pcProx+") from RFIDeas.com; the first part updates a named user's record with the hex value of his/her RFID smart card; the second part uses said value as part of the Authentication Tree described at https://github.com/javaservlets/rfIdeasAuthNode

Assumptions:

* Java SDK 1.8 or greater and
* an instance of ForgeRock 6+
* You have an existing Firebase Real-Time Database pre-configured, and *your real* account.json values have replaced the contents in the sample one provided in this project
* If running on Windows, folder delimiters in .sh files below must be changed from ":" to ";"


To run this standalone java class:

1. Use your IDE or text editor of choice and update line 35 of /src/com/rfideas/forgerock/EnrollUser to reflect your ForgeRock instance address

2. Use your IDE or text editor of choice and update line 35 of /src/com/rfideas/forgerock/BadgeTap to reflect your FireBase DB instance

3. In a terminal window run the contents of ./compile.sh

4. In a terminal window run the contents of ./enroll.sh {username}
This command will prompt you to present a badge/tap; it will read the hex values from it and write it to the {username} account in ForgeRock OpenAM (the 'sunIdentityMSISDNNumber' field for that user will then be updated with the hex value.)

5. Next, configure the RFideas Auth Tree as described at https://github.com/javaservlets/rfIdeasAuthNode

6. Lastly, in a terminal window run the contents of ./headless.sh
The authentication tree will poll and wait for the output of this last step to be written to a firebase:/headless.json message queue

Misc:
badgetap.java writes to a "queue" (Firebase Realtime DB) thus the included account-services.json file
