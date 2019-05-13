const CompressionWebpackPlugin = require("compression-webpack-plugin");

module.exports = {
    publicPath: '', // use relative paths
    outputDir: '../webcontent/admin2',
    filenameHashing: false,
    devServer: {
		host: '127.0.0.1',  
		port: 8080,		
    },
    productionSourceMap: false,
	configureWebpack: {
		performance: {  // remove warning of large size
			maxEntrypointSize: 10000000,
			maxAssetSize: 10000000,
		},
		plugins: [
			new CompressionWebpackPlugin({}),
		],		
	},
}