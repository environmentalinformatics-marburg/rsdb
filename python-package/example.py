import rsdb
from rsdb.connection import RSDB

rsdb = RSDB('http://127.0.0.1:8081')

rasterdb_list = rsdb.getRasterDBs()

print(rasterdb_list)

rasterdb = rsdb.getRasterDB('hyperspectral_forest_edge')

rasterdb_meta = rasterdb.getMeta()

print(rasterdb_meta)


vectordb_list = rsdb.getVectorDBs()

print(vectordb_list)

vectordb = rsdb.getVectorDB('plots_forest_edge')

vectordb_meta = vectordb.getMeta()

gdf = vectordb.getGeoDataFrameWGS84()

gdf.plot()
import matplotlib.pyplot as plt
plt.show()


print('...')
print('---')
