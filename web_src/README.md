# Web Interface

## Project setup
```bash
npm install
```

### Compiles and hot-reloads for development
```bash
npm run serve
```

### Compiles and minifies for production
```bash
npm run build
```

### Lints and fixes files
```bash
npm run lint
```

### download/update google fonts and icons

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

### check security of dependencies

```bash
npm audit
```

### check for updates of dependencies

```bash
npm outdated
```