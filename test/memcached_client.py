import time
from pymemcache.client.base import Client

client = Client(('localhost', 9090), timeout=5)
print("Connected to server.")

key = 'some_key'
value = 'some_value'
flags = 42

try:
    start_time = time.time()
    response = client.set(key, value, flags=flags, noreply=False)
    # uncomment the following to test expiration
    # response = client.set('some_key-3', 'some_value-3', flags=flags, noreply=False, expire=3)
    print(f"Set response: {response} in {time.time() - start_time:.4f} seconds")
except Exception as e:
    print("An error occurred:", e)

# uncomment the following to test the delete command
# try:
#     client.delete(key)
#     print(f"Delete '{key}'")
# except Exception as e:
#     print("An error occurred:", e)

# uncomment the following to test expiration
# time.sleep(3)  
try:
    start_time = time.time()
    result = client.get(key)
    print(f"The value for '{key}' is: {result.decode('utf-8') if result else None} in {time.time() - start_time:.4f} seconds")
except Exception as e:
    print("An error occurred:", e)





