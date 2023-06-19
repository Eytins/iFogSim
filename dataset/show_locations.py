import folium
import pandas as pd


def main():
    # Read the CSV file
    data = pd.read_csv('usersLocation-melbCBD_1.csv')

    # Create a map centered at the first location
    map_center = [data['Latitude'].iloc[0], data['Longitude'].iloc[0]]
    map_osm = folium.Map(location=map_center, zoom_start=10)

    # Add markers for each location
    for index, row in data.iterrows():
        lat, lon = row['Latitude'], row['Longitude']
        folium.Marker([lat, lon]).add_to(map_osm)

    # Save the map to an HTML file
    map_osm.save('locations_map.html')


if __name__ == '__main__':
    main()
