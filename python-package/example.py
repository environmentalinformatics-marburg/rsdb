import rsdb
from rsdb.connection import RsdbConnection

r = RsdbConnection('http://127.0.0.1:8081')

rs = r.getRasterDBs()

print(rs)

rasterdb = r.getRasterDB('hyperspectral_forest_edge')

meta = rasterdb.getMeta()

print(meta)

print('...')
print('---')
