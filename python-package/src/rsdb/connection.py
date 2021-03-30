import requests
from requests.auth import HTTPDigestAuth
from requests.auth import HTTPBasicAuth
import geopandas
import re
import hashlib

class RSDBConnection:
    def __init__(self, url, user='user', password='password', ssl_verify=True):
        self.url = url
        self.ssl_verify = ssl_verify
        self.user = user
        self.password = password
        self.authentication = 'none'
        self.reg = re.compile('(\w+)[=] ?"?(\w+)"?') # derived from: https://stackoverflow.com/questions/1349367/parse-an-http-request-authorization-header-with-python
        self.request('GET', '/pointdb/')
    
    def request(self, method, path, params={}):
        url = self.url + path
        if self.authentication == 'none':
            response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify)
        elif self.authentication == 'login_sha2_512':
            response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify, cookies=self.login_sha2_512_cookies)           
        elif self.authentication == 'digest':
            response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify, auth=HTTPDigestAuth(self.user, self.password))
        elif self.authentication == 'basic':
            response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify, auth=HTTPBasicAuth(self.user, self.password))            
        else:
           raise RuntimeError('Unknown authentication.')

        if response.status_code == 200:
            return response
        elif response.status_code == 401: # 401 Unauthorized
            print('Try to resolve unauthorized request...')
            auth = response.headers['WWW-Authenticate']
            #print(auth)
            if auth.startswith('login_sha2_512'):
                print('login_sha2_512')
                print(auth)
                hs = dict(self.reg.findall(auth))
                print(hs)
                server_nonce = hs['server_nonce']
                client_nonce = 'OvHcQiqR'
                user_hash_size = int(hs['user_hash_size'])
                user_salt = hs['user_salt']
                salt = hs['salt']
                h_user = hashlib.sha512((user_salt + self.user + user_salt).encode('utf-8')).hexdigest()
                print(h_user)
                h_user = h_user[-user_hash_size:]
                print(h_user)

                h_inner = hashlib.sha512((salt + self.user + salt + self.password + salt).encode('utf-8')).hexdigest()

                h = hashlib.sha512((server_nonce + client_nonce + h_inner + client_nonce + server_nonce).encode('utf-8')).hexdigest()
                url_login = self.url + "/login"
                login_params = {'user': h_user, 'server_nonce': server_nonce, 'client_nonce': client_nonce, 'hash': h}
                print(login_params)
                login_response = requests.request(method='GET', url=url_login, params=login_params, verify=self.ssl_verify, allow_redirects=False)
                print(login_response.status_code)
                if login_response.status_code == 302: # SC_FOUND --> authentication verified
                    print(login_response.cookies)
                    self.login_sha2_512_cookies = login_response.cookies
                    response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify, cookies=self.login_sha2_512_cookies)
                    if response.status_code == 200:
                        self.authentication = 'login_sha2_512'
                        return response
                    else:
                        raise RuntimeError('login_sha2_512 authentication failed.')
                else:
                    raise RuntimeError('login_sha2_512 authentication failed. Wrong user/password?') 
            elif auth.startswith('Digest'):                
                print('digest')
                response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify, auth=HTTPDigestAuth(self.user, self.password))
                if response.status_code == 200:
                    self.authentication = 'digest'
                    return response
                else:
                    raise RuntimeError('Digest authentication failed. Wrong user/password?')
            elif auth.startswith('basic'):                
                print('basic')
                response = requests.request(method=method, url=url, params=params, verify=self.ssl_verify, auth=HTTPBasicAuth(self.user, self.password))
                if response.status_code == 200:
                    self.authentication = 'basic'
                    return response
                else:
                    raise RuntimeError('Basic authentication failed. Wrong user/password?')  
            else:
               raise RuntimeError('Unknown WWW-Authenticate method')  
        else:
            raise RuntimeError(response)

    def getJSON(self, path, params={}):
        #url = self.url + path
        #resp = requests.get(url, auth=HTTPDigestAuth(self.user, self.password))
        resp = self.request(method="GET", path=path, params=params)
        data = resp.json()
        return data


class RSDB:
    def __init__(self, url, user='user', password='password', ssl_verify=True):
        self.rsdbConnection = RSDBConnection(url=url, user=user, password=password, ssl_verify=ssl_verify)

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

