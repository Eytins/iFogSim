import folium
import pandas as pd


# Read the CSV file
data = pd.read_csv('usersLocation-melbCBD_1.csv')

# Create a map centered at the first location
map_center = [data['Latitude'].iloc[0], data['Longitude'].iloc[0]]
map_osm = folium.Map(location=map_center, zoom_start=10, tiles='CartoDB Positron')

# Create a feature group to hold the markers and lines
locations_fg = folium.FeatureGroup(name='Locations')

# Add markers and lines for each location
for index, row in data.iterrows():
    lat, lon = row['Latitude'], row['Longitude']
    folium.Marker([lat, lon]).add_to(locations_fg)
    if index > 0:
        prev_lat, prev_lon = data['Latitude'].iloc[index-1], data['Longitude'].iloc[index-1]
        line = folium.PolyLine(locations=[[prev_lat, prev_lon], [lat, lon]], color='blue', weight=2)
        line.add_to(locations_fg)

# Add the feature group to the map
locations_fg.add_to(map_osm)

# Add layer control to the map
folium.LayerControl().add_to(map_osm)

# Save the map to an HTML file
map_osm.save('locations_map.html')
