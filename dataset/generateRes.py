import csv
import random

# Define constants
NUM_DEVICES = 1300
MAX_LATITUDE = -37.809041
MIN_LATITUDE = -37.820744
MAX_LONGITUDE = 144.975705
MIN_LONGITUDE = 144.951955

# Create CSV file and write header
with open('edgeResources-melbCBD.csv', 'w', newline='') as csvfile:
    fieldnames = ['ID', 'Latitude', 'Longitude', 'Block', 'Level', 'Parent', 'State', 'Details']
    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
    writer.writeheader()

    # Create Data Center
    writer.writerow({'ID': 0, 'Latitude': -37.8136, 'Longitude': 144.9631, 'Block': 0, 'Level': 0, 'Parent': -1, 'State': 'VIC', 'Details': 'DataCenter'})

    # Generate devices
    device_id = 1
    for _ in range(NUM_DEVICES - 1):  # Exclude Data Center
        latitude = random.uniform(MIN_LATITUDE, MAX_LATITUDE)
        longitude = random.uniform(MIN_LONGITUDE, MAX_LONGITUDE)
        block = random.randint(1, 12)  # Assuming 12 blocks
        level = random.randint(1, 2)  # Level 1 or 2
        state = 'VIC'

        if level == 1:
            parent = 0  # Data Center is the parent of proxies
            details = f'Block{block} Proxy'
        else:
            parent = random.choice(range(1, device_id))  # Random parent from existing devices
            details = f'Block{block} Gateway'

        writer.writerow({'ID': device_id, 'Latitude': latitude, 'Longitude': longitude, 'Block': block, 'Level': level, 'Parent': parent, 'State': state, 'Details': details})
        device_id += 1
