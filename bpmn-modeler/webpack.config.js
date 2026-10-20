const path = require('path');

module.exports = {
  entry: './src/renderer.js',
  target: 'electron-renderer',
  output: {
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'dist'),
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: { presets: ['@babel/preset-env'] },
        },
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader'],
      },
      {
        test: /\.(bpmn|xml)$/,
        type: 'asset/source',
      },
      {
        test: /\.(png|svg|jpg|gif|woff|woff2|ttf|eot)$/,
        type: 'asset/resource',
      },
    ],
  },
  resolve: { extensions: ['.js'] },
  mode: 'development',
  devtool: 'source-map',
};
