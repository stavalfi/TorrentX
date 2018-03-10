# Task Schedule
### Feature 1: Getting started. `IN PROGRESS`

1. Understand what .torrent files are.
2. How to communicate with a trackerUrl.
3. How to communicate with a peer. 
4. Paper - Introduction and related work.
-----

##### PBI-1: Create work space. `DONE`

1. Git repository – GitHub.
2. Configure branch protection + pull request.
3. Create automatic build: compilation + Units tests + cucumber tests – Travis-CI.

-----

##### PBI-2: Shachar training. `IN PROGRESS`

1. Git - add,commit,branch,checkout,merge,push,fetch. `DONE` (Shachar)
2. Github - pull requests. `DONE` (Shachar)
3. Travis-CI - understand what it is and how to run a build. `NOT STRATED` (Shachar)
4. Cucumber - feature,scenario,steps,step-definition,debug steps. `NOT STRATED` (Shachar)

-----

##### PBI-3: Fully understand torrent file structure. `DONE`

1. List of information types which are saved in a .torrent file. (Shachar, Stav)
2. Understand how much parts there are for a file. Where can we get this information for each torrent file. (Shachar, Stav)
3. What library in java we need to use to read and analyze .torrent files. (Shachar, Stav)

-----

##### PBI-4: Get details about a given trackerUrl. `DONE` 

1. Find the protocol and udpPort which enable us to communicate with a given trackerUrl. (Stav)
2. List the methods we can run on a given trackerUrl. (Stav)
3. Get list of peers information from this trackerUrl on a specific torrent. (Stav)
4. Extract a peers IP from a seeder information. (Stav)

-----

##### PBI-5: Add additional CI - `CircleCI`. `DONE` 

1. GitHub pull request can be approved if and only if the build is pass in `CircleCI` and `TravisCI`. (Stav)

-----

##### PBI-6: Learn `Reactor 3` library. `DONE` 

1. Creating a New Sequence. (Stav)
2. Transforming an Existing Sequence. (Stav)
3. Peeking into a Sequence. (Stav)
4. Handling Errors. (Stav)
5. Writing tests. (Stav)

-----

##### PBI-7: Design and implement reactive API for providing trackers. `DONE` 

1. Design a reactive API which provide trackers we connected to them. (Stav)
    * Connect
    * Announce
    * Scrape
2. Implementing tests. (Stav)
3. Implementing the API. (Stav)

------
##### PBI-8: Learn about peers inside the specification. `DONE` 

1. Find the protocol and udpPort which enable us to communicate with a seeder.
2. List the methods we can run on a given seeder.

-----
##### PBI-9: Design and implement reactive API for providing peers. `DONE` 

1. Design a reactive API which provide peers we connected to them. (Stav)
The API will let us listen for incoming messages and provide us a way to send messages to them.
    * Handshake
    * BitField
    * Cancel
    * Choke
    * Have
    * Interested
    * KeepAlive
    * NotInterested
    * Piece
    * Port - We are not supporting this at the moment.
    * Request
    * Unchoke
    
2. Implementing tests. (Stav)
3. Implementing the API. (Stav)

-----

##### PBI-10: Design a torrent manager API to handle the download & upload of a specific torrent. `DONE`

1. Learn about basic bittorrent algorithms. (Stav)
2. Design an abstract API for download and upload a torrent. (Stav)

-----

##### PBI-11: Paper - Part 2. `DONE`

1. Learn how to work with latex. (Shachar, Stav)
2. Create a paper on:  (Shachar, Stav)
    * What are we building.
    * How client torrent application work.
    * What our application do,add,won't do compared to other client torrent applications.
    * How our application will work including technologies.
