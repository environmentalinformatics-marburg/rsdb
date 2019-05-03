const CompressionWebpackPlugin = require("compression-webpack-plugin");

module.exports = {
    baseUrl: '', // use relative paths
    outputDir: '../webcontent/admin2',
    filenameHashing: false,
    devServer: {
        //proxy: 'http://localhost:8081'
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