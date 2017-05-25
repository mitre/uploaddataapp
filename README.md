UploadDataApp
=============

This app is designed to test app security analysis products and services.

It contains a variety of deliberately inserted privacy violating behaviors and security weaknesses.

Many are listed below along with the corresponding security requirement in the NIAP Protection Profile for Application Software
(https://www.niap-ccevs.org/Profile/Info.cfm?id=394)

More details can be found in our report , available at https://github.com/mitre/vulnerable-mobile-apps/blob/master/analyzing-effectiveness-mobile-app-vetting-tools-report.pdf

* Access to device hardware resources (FDP_DEC_EXT.1.1)
** Activates device microphone for 5 seconds, writes audio to a file, and sends the file to a remote server
** Obtains device physical location and sends it to a remote server
* Access to device sensitive information repositories (FDP_DEC_EXT.1.2) - gathers data, writes to a file, and sends to remote server
** Whether the Android Debug Bridge (USB debugging) is on or off
** Whether installation of non-Google Play Store apps is allowed or disallowed
** The device's Android ID, IMSI, IMEI, phone number, and current IP addresses
** Names of all apps installed on the device
** Contact list entries
** Call logs
** Names of all files stored in external storage
* Insecurely writing sensitive application data to device storage (FDP_DAR_EXT.1.1 and FMT_CFG_EXT.1.2)
** Files containing the data described above are written to the app's internal storage directory with
insecure file permissions (world readable and world writable) and are also written to device external storage
* Insecure network communication (FDP_DEC_EXT.1.4, FTP_DIT_EXT.1.1, FCS_HTTPS_EXT.1.3, FCS_TLSC_EXT.1.2)
** The app can be configured to send data over HTTP (insecure plaintext) or HTTPS
*** When HTTPS is used, certificate validation is disabled, and hostname checking is not performed, making the
communication susceptible to man-in-the-middle attack
* Insecure storage of authentication credentials (FMT_CFG_EXT.1.1)
** A username and password are embedded in the app code and used for HTTP Basic Authentication when uploading the data described above
** An AES key is embedded in the app code and used to encrypt the above data (it is stored and sent both unencrypted and encrypted)
* Other
** AES-CBC encryption uses a static, predictable initialization vector (IV) embedded in the app code
** No MAC is used in conjunction with the AES-CBC encryption

## Setup
This project can easily be imported into Android Studio. 
It must be run on a device with **API level <= 22** (5.1 Lollipop)
due to the new runtime permissions system introduced in API 23. (6.0 Marshmallow)
