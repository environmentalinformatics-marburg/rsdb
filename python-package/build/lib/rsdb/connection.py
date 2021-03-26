import requests
from requests.auth import HTTPDigestAuth
import geopandas

class RSDBConnection:
    def __init__(self, url, user='user', password='password'):
        self.url = url
        self.user = user
        self.password = password

    def getJSON(self, path, params={}):
        url = self.url + path
        resp = requests.get(url, auth=HTTPDigestAuth(self.user, self.password))
        data = resp.json()
        return data


class RSDB:
    def __init__(self, url, user='user', password='password'):
        self.rsdbConnection = RSDBConnection(url=url, user=user, password=password)

    def getRasterDBs(self):
        path = '/rasterdbs.json'
        data = self.rsdbConnection.getJSON(path)
        return data['rasterdbs']

    def getRasterDB(self, name):
        return RasterDB(self.rsdbConnection, name)

    def getVectorDBs(self):
        path = '/vectordbs'
        data = self.rsdbConnection.getJSON(path)
        return data['vectordbs']

    def getVectorDB(self, name):
        return VectorDB(self.rsdbConnection, name)               


class RasterDB:
    def __init__(self, rsdbConnection, name):
        self.rsdbConnection = rsdbConnection
        self.name = name

    def getMeta(self):
        path = '/rasterdb/' + self.name + '/meta.json'
        data = self.rsdbConnection.getJSON(path)
        return data


class VectorDB:
    def __init__(self, rsdbConnection, name):
        self.rsdbConnection = rsdbConnection
        self.name = name

    def getMeta(self):
        path = '/vectordbs/' + self.name
        data = self.rsdbConnection.getJSON(path)
        return data['vectordb']

    def getGeoDataFrameWGS84(self):
        path = '/vectordbs/' + self.name + '/geometry.json'
        params = {'epsg': 4326}
        data = self.rsdbConnection.getJSON(path=path, params=params)
        gdf = geopandas.GeoDataFrame.from_features(data['features'])
        #gdf.crs = "EPSG:4326" # error setting crs
        return gdf            

