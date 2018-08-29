# cse306-project-2-Memory
1. You have to implement the LRU page replacement algorithm (the real LRU, with timestamps,
not the clock algorithm) and dirty-bit optimization (if you do not optimize for clean
frames, OSP2 issues warnings, which is not OK).

2. In addition, also implement the following proactive page cleaning daemon. The daemon
should kick in every 20,000 clock ticks and swap out the 5 least recently used dirty frames.
(If the number of dirty frames is less than 5, swap out whatever there is.)
