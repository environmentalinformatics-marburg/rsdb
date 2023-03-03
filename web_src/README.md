# Web Interface

## Project setup
```bash
npm install
```
## Compiles and hot-reloads for development
```bash
npm run serve
```

## Compiles and minifies for production
```bash
npm run build
```

## Lints and fixes files
```bash
npm run lint
```

## download/update google fonts and icons

By command line:
```bash
npm install -g get-google-fonts@latest
get-google-fonts -i "https://fonts.googleapis.com/css?family=Roboto:100:300,400,500,700,900|Material+Icons"
```
Downloads fonts to folder 'fonts' in current directory. Copy files to folder: '...rsdb/web_src/public/fonts'


Or by node commands:
 (not working anymore ?)

```bash
npm install get-google-fonts

node

const GetGoogleFonts = require('get-google-fonts');

new GetGoogleFonts().download('https://fonts.googleapis.com/css?family=Roboto:100:300,400,500,700,900|Material+Icons')
```

## check security of dependencies

```bash
npm audit
```

## check for updates of dependencies

```bash
npm outdated
```


## New node.js versions

Fix at newer node.js versions (e.g. 18) for error "digital envelope routines::unsupported":

Fix is integrated in file "package.json", runs on windows only.
To run on ubuntu see:  https://gankrin.org/how-to-fix-error-digital-envelope-routinesunsupported-in-node-js-or-react/


Or set env variable before running build commands. (For this you need to remove the fix in "package.json" file first.)

Replace the (for Windows fixed) section:
```JSON
  "scripts": {
    "serve": "set NODE_OPTIONS=--openssl-legacy-provider & vue-cli-service serve",
    "build": "set NODE_OPTIONS=--openssl-legacy-provider & vue-cli-service build",
    "lint": "set NODE_OPTIONS=--openssl-legacy-provider & vue-cli-service lint"
  },
```

with
```JSON
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint"

  },
```

Windows console cmd:
```
set NODE_OPTIONS=--openssl-legacy-provider
```


Ubuntu console bash:
```bash
export NODE_OPTIONS=--openssl-legacy-provider
```
