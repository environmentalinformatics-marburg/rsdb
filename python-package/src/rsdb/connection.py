import requests
from requests.auth import HTTPDigestAuth

class RsdbConnection:
    def __init__(self, url):
        self.url = url

    def getRasterDBs(self):
        url = self.url + '/rasterdbs.json'
        resp = requests.get(url, auth=HTTPDigestAuth('user', 'pass'))
        data = resp.json()
        return data['rasterdbs']

    def getRasterDB(self, name):
        return RasterDB(self.url + '/rasterdb/' + name)


class RasterDB:
    def __init__(self, url):
        self.url = url

    def getMeta(self):
        url = self.url + '/meta.json'
        resp = requests.get(url, auth=HTTPDigestAuth('user', 'pass'))
        data = resp.json()
        return data

