import matplotlib.pyplot as plt

file_response_times = []
with open('response_times_f1.txt', 'r') as file:
    next(file)
    for line in file:
        parts = line.strip().split(',')
        if len(parts) == 4:
            client_id = int(parts[0])  
            command = parts[1]
            key = parts[2]
            response_time = int(parts[3]) 
            file_response_times.append((client_id, command, key, response_time))

file_set_times = [rt[3] for rt in file_response_times if rt[1] == 'set']
file_get_times = [rt[3] for rt in file_response_times if rt[1] == 'get']


kv_response_times = []
with open('response_times_kv1.txt', 'r') as file:
    next(file)
    for line in file:
        parts = line.strip().split(',')
        if len(parts) == 4:
            client_id = int(parts[0])  
            command = parts[1]
            key = parts[2]
            response_time = int(parts[3]) 
            kv_response_times.append((client_id, command, key, response_time))

kv_set_times = [rt[3] for rt in kv_response_times if rt[1] == 'set']
kv_get_times = [rt[3] for rt in kv_response_times if rt[1] == 'get']

plt.figure(figsize=(10, 6))

plt.plot(file_set_times, label='File Server Set Response Times', color='blue', linestyle='-', marker='o')
plt.plot(file_get_times, label='File Server Get Response Times', color='blue', linestyle='--', marker='x')

plt.plot(kv_set_times, label='KV Server Set Response Times', color='red', linestyle='-', marker='o')
plt.plot(kv_get_times, label='KV Server Get Response Times', color='red', linestyle='--', marker='x')

plt.xlabel('Request Number')
plt.ylabel('Response Time (ms)')
plt.title('Response Times for Set and Get Commands (File Server vs. KV Server)')
plt.legend()
plt.grid()

plt.savefig('response_times_plot_1.png')
plt.close()
