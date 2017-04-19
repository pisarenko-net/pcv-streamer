# Routines to analyze Power Commander's USB packets

The goal was to extract RPM and throttle values. The program has a couple of functions that helped me to decode values from the USB packets exchanged between Power Commander 5 and its Windows software.

Folder `txt-dumps` contains text-formatted exports from HHD Device Monitoring Studio software. I used these exports to figure out where particular data is placed in USB packets.

Each USB packet is 64 bytes long. Bytes are placed in little endian order. So, for example, “0x0500” is decoded as 5 and not as 1280. To convert to decimal one needs to reverse the bytes first, i.e. “0x0005”. 

Packet format (bytes from left to right):
 * 4 bytes - nonce (random integer) to associate responses with requests
 * 2 bytes - command ID
 * 2 bytes - payload length
 * X bytes - payload
 * zeroes or junk to fill a 64 bit packet

For more information refer to http://pisarenko.net/blog/2017/04/17/internet-connected-motorcycle-project-part-2/

A great deal of knowledge can be learned by decompiling Dynojet Android app: https://play.google.com/store/apps/details?id=com.dynojet.c3mobile&hl=en
