# TorrentX

<img src="https://i.imgur.com/bgb8Dq7.png" height="25%"/>

[![Build Status](https://travis-ci.org/stavalfi/TorrentX.svg?branch=master)](https://travis-ci.org/stavalfi/TorrentX)
[![CircleCI](https://circleci.com/gh/stavalfi/TorrentX.svg?style=svg)](https://circleci.com/gh/stavalfi/TorrentX)

TorrentX is a fully reactive client side software for downloading and uploading torrents.

1. [Introduction](#introduction)  
2. [Enviroment](#enviroment)  
3. [Dependencies](#dependencies)  

## Introduction

![](https://i.imgur.com/mGUDESi.gif)


BitTorrent is a peer-to-peer protocol, which means that the computers in a BitTorrent “swarm” (a group of computers downloading and uploading the same torrent) transfer data between each other without the need for a central server, where all the information passes through a particular server.
The trackerUrl server keeps track of where file copies reside on peer machines, which ones are available at the time of the clients request, and helps coordinate efficient transmission and reassembly of the copied file.

In recent years, BitTorrent has emerged as a scalable peer-to-peer file distribution mechanism.
Because of the great popularity of the BitTorrent there is a lot of interest among the scientific community on whether it is possible to improve the performance of this protocol.
So many measurement and analytical studies have published suggestions for different algorithms that achieve performance improvements.


## Enviroment
* Linux 14,16, Windows 7, macOS High Sierra
* Java 8
* Maven 3

## Dependencies
* [Project Reactor](https://github.com/reactor/reactor-core)
* [Cucumber](https://cucumber.io/) - 200+ tests
* [PaperTrail](https://papertrailapp.com/) - Cloud logger
