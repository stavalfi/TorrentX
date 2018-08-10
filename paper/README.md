# TorrentX

##### Stav Alfi | 204031397

1. [Introduction](#introduction)  
2. [The Need](#the-need)  
3. [The BitTorrent protocol ](#the-bittorrent-protocol)  
4. [TorrentX implementation ](#torrentx-implementation )  
5. [Design patterns](#design-patterns)
6. [TorrentX capabilities](#torrentx-capabilities)
7. [Enviroment](#enviroment)
8. [Main dependencies](#main-dependencies)
9. [Tests](#tests)
10. [Logs](#logs)
11. [Builds](#builds)
12. [Network](#network)
13. [GUI](#gui)

## Introduction

TorrentX is the first Implemention of a fully reactive client side software for downloading and uploading data from within multiple machines by implementing the BitTorrent protocol. 

BitTorrent is a peer-to-peer protocol, which means that the computers in a BitTorrent “swarm” (a group of computers downloading and uploading the same torrent) transfer data between each other without the need for a central server, where all the information passes through a particular server. The tracker server keeps track of where file copies reside on peer machines, which ones are available at the time of the clients request, and helps coordinate efficient transmission and reassembly of the copied file.

#### The Need

A BitTorrent download differs from a classic download in several fundamental ways:
* BitTorrent makes many small data requests over different IP connections.
* BitTorrent downloads in a random or in a "rarest-first" approach that ensures high availability

In recent years, BitTorrent has emerged as a scalable peer-to-peer file distribution mechanism. Because of the great popularity of the BitTorrent there is a lot of interest among the scientific community on whether it is possible to improve the performance of this protocol. So many measurement and analytical studies have published suggestions for different algorithms that achieve performance improvements.

TorrentX core algorithms are latest and the best currently avialble and will be developed by us from scratch.

##  The BitTorrent protocol 

To better understand how a given torrent algorithm works, we must
understand what types of messages are transfered between peers in the p2p
(peer-to-peer) protocol.

Every message is described in the p2p protocol by
what its size must be, what its content and by what order.
1. The keep-alive message is a message with zero bytes, specified with the
length prefix set to zero. There is no message ID and no payload (extra
data). Peers may close a connection if they receive no messages (keep-alive
or any other message) for a certain period of time, so a keep-alive message
must be sent to maintain the connection alive if no command has been sent
for a given amount of time. This amount of time is generally two minutes.
2. Choke message means that if peer(i) sends it to peer(j), then peer(i) will
ignore any future requests for pieces. The choke message is fixed-length
and has no payload.
2
3. Unchoke message means that if peer(i) sends it to peer(j), then peer(i)
will no longer ignore any future requests for pieces. The unchoke message
is fixed-length and has no payload.
4. Interested message means that if peer(i) sends it to peer(j), peer(i)
wants peaces which peer(j) has to offer. The interested message is
fixed-length and has no payload.
5. Not interested message means that if peer(i) send it to peer(j), peer(i)
does not want any peaces which peer(j) have to offer.The not interested
message is fixed-length and has no payload.
6. The have message is fixed length. The payload is the zero-based index
of a piece that has just been successfully downloaded and verified via the
hash.
7. The bitfield message may only be sent immediately after the
handshaking sequence is completed, and before any other messages are
sent. It is optional, and need not be sent if a client has no pieces.
The bitfield message is variable length, /The payload is a bitfield
representing the pieces that have been successfully downloaded. The high
bit in the first byte corresponds to piece index 0. Bits that are cleared
indicated a missing piece, and set bits indicate a valid and available piece.
Spare bits at the end are set to zero.
A bitfield of the wrong length is considered an error. Clients should drop
the connection if they receive bitfields that are not of the correct size, or if
the bitfield has any of the spare bits set.
8. The request message is fixed length, and is used to request a block. The
payload contains the following information: Index: integer specifying the
zero-based piece index. Begin: integer specifying the zero-based byte offset
within the piece. Length: integer specifying the requested length.
9. The piece message is variable length, where X is the length of the block.
The payload contains the following information: Index: integer specifying
the zero-based piece index. Begin: integer specifying the zero-based byte
offset within the piece. Block: block of data, which is a subset of the piece
specified by index.
3
10. The cancel message is fixed length, and is used to cancel block
requests. The payload is identical to that of the ”request” message. It is
typically used during ”End Game” (see the Algorithms section below).
Downloading peers achieve rapid download speeds by requesting multiple
pieces from different computers simultaneously in the swarm. The torrent
application downloads each piece and combines them together. Once the
BitTorrent client has some data, it can then begin to upload that data to
other BitTorrent clients in the swarm.

The next step is to understand what a _Tracker_ is and how can we communicate with him. A _Tracker_ is a server with a signle job - to track over all the peers who posses files for transfer. To each file, it saved all the ip address of all the peers which currently download or upload this file. When a new peer is trying to download a file, he ask the tracker who can he download from. The communication protocol between a peer and a tracker is:

1. Connection request - The peer is trying to identify him self to the tracker. 
2. Announce request - The peer is requesting for the peers list for a specific torrent. By doing so, he also becomes a seeder.
3. Scrape request - The peer ask the tracker if he have any update after the initial Announce request about the peers list for a specific torrent.

## TorrentX implementation 

TorrentX is written in Java 8 and implemented in a fully [functional style](https://en.wikipedia.org/wiki/Functional_programming) and [event driven programming](https://en.wikipedia.org/wiki/Event-driven_programming) to support maximum concurrency and parallism without any additional synchronization tools. The libarery I use to support this kind of implementation is [_Reactor project_](https://github.com/reactor/reactor-core) under the [specification of th elatest reactive programming contract](http://www.reactive-streams.org/).

I also make a havy use of [_Redux_ design pattern](https://redux.js.org/introduction) to manage the state of my application in any given time.

As a consequence of the above, TorrentX is fully [asynchronous](https://en.wikipedia.org/wiki/Asynchrony_(computer_programming)) implementation of the BitTorrent protocol.

## Design patterns

##### [Redux](https://redux.js.org/introduction) 
1. Multiple modules cuncurrently subscribe to changes in the download/upload status and some of them are designed to cuncurrently change the current status. To support such a requeirement; to prevent modules to change the state without all the relevant modules to know what the change, I use Redux.
2. Any Bittorent implemnation is responsible to receive and send blocks of data (16_000 bytes to be presise). Each allocation for receving from the network or preparing a block from the local filesystem to be sent to a peer is a result of an allocation. Java programming langauge doesn't allow to menually free allocations. To prevent a senario when my proccess is out of memory (It will for any download of a file larger then 8 GB), I must manage the allocation and the frees my self. When multiple modules are trying to allocate blocks from a fixed size pool, I need to let them know when a free block is avaliable. The currently avaible allocations are my state so Redux is a good fit here also.

##### Builder

In every use of redux, I use builder to build the new state. I have a required fields and an optional fields.

##### Visitor

1. When receiving a response from a peer, I need to check what is the type of the response and by that build the response object. If it's a _Piece Message_ then I need to allocate a memory for the block I need to read. If not, I can safly build the massage.
2. When sending a message to a peer, I need to send it differently if it's a _Piece Message_.

##### Strategy

I use Strategy in almost all the project to implement all the algorithms for downloading and uploading torrents. Also I use it for algorithms which responsible for intracting with the local filesystem to read and save blocks. 

##### Singleton

Each torrent we download is represented by a download object. To save all the download objects and give a clean API of how to create, search and remove them, I use a singletone object of a class who is responsible for all those tasks.

##### Factory

I use it in the tests to build a fake message to a peer or to a tracker by the type of the message. (2 factories).

> General note: I use more design patterns without actually implement them so I didn't list them. Such as: Observer and Iterator.

## TorrentX capabilities

1. Communicate with trackers and retrieve from them the list of peers.
2. Communicate with peers and request from them the parts of a specific torrent.
3. Be able to add in realtime more peers to each torrent.
4. Peers black list.
5. Accepting incoming peers connetion so peers reach me also.
6. Upload a torrent to multiple peers.
7. Recover from trackers errors - shutdown, UDP messages curraption and more.
8. Recover from peers errors - Not responding, not cuaparating with me (only requets but doesn't give back).

#### Enviroment
* Linux 16.04
* Windows 10 

#### Main dependencies
* [Project Reactor](https://github.com/reactor/reactor-core)

#### Tests
* [Cucumber](https://cucumber.io/) - 200+ tests

#### Logs
* [PaperTrail](https://papertrailapp.com/) - Cloud logger

#### Builds
* Git
* GitHub
* [Circle CI](https://circleci.com/)
* [Travis CI](https://travis-ci.org/)

## Network

The peers communicate using TCP/IP protocol while the tracker communication is done using UDP protocol.

## GUI

Using the command line. This is because a time-limitation.