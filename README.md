# Interoperable TFTP client and server implementation

## Table of Contents
1. [Technologies Used](#Technologies)
2. [Project Description](#Description)
3. [Folder Structure](#Folder)

## Technologies
Languages:

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)

Miscellaneous: 

![Obsidian](https://img.shields.io/badge/Obsidian-%23483699.svg?style=for-the-badge&logo=obsidian&logoColor=white) ![Ubuntu](https://img.shields.io/badge/Ubuntu-E95420?style=for-the-badge&logo=ubuntu&logoColor=white)

## Description
This repo contains my coursework for the Computer Networks module I studied while at university. It is a Gradle / Java project that contains my full implementation of a TFTP client and server. It has a UDP version which is fully operable with third party clients (like [Tftpd64](https://pjo2.github.io/tftpd64/) and the built in Linux client) and a TCP version. The TCP version is my own implementation of the specification if it were to exist in a TCP form. Please note that the UDP version is compliant with [RFC 1350](https://www.ietf.org/rfc/rfc1350.txt).

*My final grade for this coursework was **69/100***.

## Folder
The structure of the folder is as follows:

**TFTP-UDP-Server** - this folder contains the interoperable TFTP server.

**TFTP-UDP-Client** - this folder contains the interoperable TFTP client.

**TFTP-TCP-Server** - this folder contains the custom TCP server implementation.

**TFTP-TCP-Client** - this folder contains the custom TCP client implementation.
