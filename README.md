# Memcached-lite
Memcached-lite is an implementation of a distributed memory object caching system that uses a Memcached client-server model, using Google Cloud instances. 
## Operations
The client and the server processes are each hosted on a VM instance. The client uses the external IP of the server to connect to it. The source files can be found under src/main/java/
The following are the steps to run the client and server as separate processes:

1. **Build the app**
```
./gradlew build
```
2. **Run the server**
```
./gradlew runServer
```
3. **Run the client**
```
./gradlew runClient
```
## Performance
### Memcached-lite
For basic performance measurement, the server measures the time it takes it to process requests.
The client, on the other hand, measure end-to-end latency. The average server performance after 10 experiments can be outlined as follows:
• SET request processing: 630 ms.
• GET request processing: 304 ms.
• Response time: 950 ms.

The performance of the server is slightly worse than its performance when tested locally, which
makes sense seen that this time around network latency comes into play.
### KV Store
Another server that uses one of Google’s Key-Value store; Firestore, is created. It is called
KVServer and can be run as:
```
./gradlew runKVServer
```
The performance of the server is, on average, as follows:
• SET request processing: 410 ms.
• GET request processing: 81 ms.
• Response time: 840 ms.

For the same workload, the performance of the Key-Value store server is relatively better than the performance of the Memcached server. This makes sense seen that the first uses in-memory storage while the latter persists to the files.


To test the performance of both the KV store server and the memcached server, Client2 was used.
It creates concurrent clients that connect to the server, simulating higher workloads. It turns out that under increasing loads the KV server surpasses the memcached server with an noticeable
performance gap. The results and intricacies of these experiments can be summarized as plots as
detailed in the next section.
Client2.java can be found under test/java/
## Response Time
The experiments presented in this section involve using Client2 to compare the latencies of the KV store server and the memcached server. The first workload, Fig. 1, uses 3 clients, 3 requests per client, and 1000 ms as a delay between requests. The second workload, Fig. 2, uses 5 clients, 5 requests per client, and 500 ms as a delay between requests. The KV server performs remarkably
better with a total average 300 ms latency, compared to upwards of 1000 ms for the memcached
server. This demonstrates the difference in latency between memory storage and disk storage.
The first workload: 3 clients, 3 requests per client, and a delay of 1000 ms between requests. The arrival rate $$\lambda_1$$ can be calculated as:
\[
\lambda_1 = \frac{1}{\text{delay}} = \frac{1}{1} = 1 \, \text{request/second},
\]

with a total arrival rate for all clients of:
\[
\Lambda_1 = 9 \, \text{requests/second}.
\]

#### Memcached Server Utilization and Latency

For the Memcached server, with an estimated service rate of:

\[
\mu_{\text{memcached}} = \frac{9}{0.63} = 14.3 \, \text{requests/second},
\]

the utilization is calculated as:

\[
\rho_{\text{memcached}} = \frac{\Lambda_1}{\mu_{\text{memcached}}} = \frac{9}{14.3} \approx 0.63.
\]

The average latency for the Memcached server is:

\[
W_{q, \text{memcached}} = \frac{\rho_{\text{memcached}}}{\mu_{\text{memcached}}(1 - \rho_{\text{memcached}})} = \frac{0.63}{14.3 (1 - 0.63)} \approx 0.1190 \, \text{seconds}.
\]

#### KV Server Utilization and Latency

For the KV server, with an estimated service rate of:

\[
\mu_{\text{KV}} = \frac{9}{0.410} = 21.95 \, \text{requests/second},
\]

the utilization is given by:

\[
\rho_{\text{KV}} = \frac{\Lambda_1}{\mu_{\text{KV}}} = \frac{9}{21.95} \approx 0.410,
\]

leading to an average latency of:

\[
W_{q, \text{KV}} = \frac{\rho_{\text{KV}}}{\mu_{\text{KV}}(1 - \rho_{\text{KV}})} = \frac{0.410}{21.95 (1 - 0.410)} \approx 0.0316 \, \text{seconds}.
\]

So, even theoretically in accordance with the M/M/1 queueing model, the KV server has smaller
latency than the memchached server; largely due to the storage type.
