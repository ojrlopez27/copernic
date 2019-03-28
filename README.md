# COPERNIC

Status for master branch:

[//]: # (this is a comment: see this link for badges using travis-CI, codecov, etc: https://github.com/mlindauer/SMAC3/blob/warmstarting_multi_model/README.md) 
![build](https://img.shields.io/badge/build-passing-green.svg?cacheSeconds=2592000) 
![test](https://img.shields.io/badge/test-passing-green.svg?cacheSeconds=2592000) 
![coverage](https://img.shields.io/badge/coverage-90%25-yellowgreen.svg?cacheSeconds=2592000) 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/82ae4c2ab01e40509051a9f115571e92)](https://www.codacy.com/app/ojrlopez27/copernic?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ojrlopez27/copernic&amp;utm_campaign=Badge_Grade)

Implementation:

![version](https://img.shields.io/badge/version-2.0-blue.svg?cacheSeconds=2592000)
![language](https://img.shields.io/badge/language-Java-yellowgreen.svg?cacheSeconds=2592000) 
![language](https://img.shields.io/badge/language-Android-yellowgreen.svg?cacheSeconds=2592000) 
![dependencies](https://img.shields.io/badge/dependencies-MUF-orange.svg?cacheSeconds=2592000)

(See a demo here: <a href="https://drive.google.com/open?id=1UTuc8qolEryF4xxTRj8o7UvSRWjOaPyn">video</a>)

## Overview

**COPERNIC** stands for **CO**gnitively-inspired **P**ervasive middl**E**ware for eme**R**ge**N**t serv**I**ce **C**omposition. COPERNIC provides:

1. A Communication layer for:
	1.1 Discovering devices in a proximity network using sockets UDP
	1.2 Communicating with remote devices and services in WAN networks through sockets TCP
	1.3 Messaging library with minimal latency footprint
2. A Service layer for:
	2.1 Service Management (registration, lookup, etc.)
	2.2 Service Discovery through Semantic descriptions (work in progress)
	2.3 Service composition using hybrind methods (classic AI planning methods and spreading activation dynamics) in pervasive environments
	2.4 Machine Learning to improve adaptivity of service composition (work in progress)

3. A Session Management layer for:
	3.1 Session control: creation and removal (by expiration) of sessions
	3.2 Cross-session coordination: a single user shares the same session across multiple devices (e.g., tablets, phones, smartwatches, etc.)
	3.3 Cross-user coordination: service composition may require multiple users involved in the construction of emergent plans.


COPERNIC Architecture:
<p align="center">
    <img align="center" width="75%" src="copernic-architecture.png" alt="COPERNIC Architecture" title="COPERNIC Architecture"</img>   
</p>

Distributed Architecture:
<p align="center">
    <img align="center" width="75%" src="cross-user-cross-session.png" alt="Cross-user, cross-session coordination" title="Cross-user, cross-session coordination"</img>   
</p>


