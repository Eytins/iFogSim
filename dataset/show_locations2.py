import folium
import pandas as pd

# Read the edgeResources CSV file
edge_data = pd.read_csv('edgeResources-melbCBD130.csv')

# Read the usersLocation CSV file
user_data = pd.read_csv('usersLocation-melbCBD_1.csv')

# Create a map centered at the first location in edgeResources data
map_center = [edge_data['Latitude'].iloc[0], edge_data['Longitude'].iloc[0]]
map_osm = folium.Map(location=map_center, zoom_start=10, tiles='CartoDB Positron')

# Define a dictionary to map levels to colors for edgeResources data
level_colors = {
    0: 'blue',
    1: 'green',
    2: 'orange',
    3: 'red',
    # Add more colors for additional levels if needed
}

# Create a feature group for each level of edgeResources data
level_fgs = {}

# Add markers for each location in edgeResources data
for index, row in edge_data.iterrows():
    lat, lon, level = row['Latitude'], row['Longitude'], row['Level']
    color = level_colors.get(level, 'gray')  # Default to gray if level not found in level_colors
    marker = folium.Marker([lat, lon], popup=f'Level: {level}', icon=folium.Icon(color=color))
    marker.add_to(level_fgs.setdefault(level, folium.FeatureGroup(name=f'Level {level}')))

# Add all level feature groups to the map
for level, fg in level_fgs.items():
    fg.add_to(map_osm)

# Connect userLocation markers with lines in sequence
user_fg = folium.FeatureGroup(name='User Locations')
for i in range(len(user_data) - 1):
    lat1, lon1 = user_data['Latitude'].iloc[i], user_data['Longitude'].iloc[i]
    lat2, lon2 = user_data['Latitude'].iloc[i + 1], user_data['Longitude'].iloc[i + 1]
    folium.Marker([lat1, lon1], popup='User Location', icon=folium.Icon(color='purple')).add_to(user_fg)
    folium.PolyLine(locations=[[lat1, lon1], [lat2, lon2]], color='purple', weight=2).add_to(user_fg)

# Add the user feature group to the map
user_fg.add_to(map_osm)

# Add layer control to the map
folium.LayerControl().add_to(map_osm)

# Save the map to an HTML file
map_osm.save('locations_map.html')
