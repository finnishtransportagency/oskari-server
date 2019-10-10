# Oskari

[Oskari](http://www.oskari.org/) Map Application Framework aims to provide a framework and a collection of functionality-enhancing bundles and plugins for rapid development of feature-rich GI web applications.

Documentation available at [http://www.oskari.org].

This repository holds the frontend code and a sample application for Oskari. Developing the frontend requires an oskari-server to be running that responds to the XHR requests made by the frontend. You can download a pre-compiled copy of the server from [http://www.oskari.org/download] or checkout the source code in the oskari-server repository. Any customized application should use the oskari-server-extension template as base for customized server and create an app-specific repository for the frontend. Oskari frontend code is built using Webpack and the same build system can be easily used for customized apps.

## Preparations

Make sure you have at least Node 8.x / NPM 5.x. Run `npm install` in the frontend repository root.
Make sure you have oskari-server running on localhost port 8080 (can be customized on webpack.config.js).

### App composition

An Oskari frontend application consists of bundles that are defined in the miniferAppSetup.json for each app (an example can be found under applications/sample/servlet). Only bundles referenced here can be instantiated at runtime. Bundles that are used only in a limited part of the app (for example admin tools) can be configured to load dynamically at runtime. This will decrease the size of the main app JS bundle. To enable dynamic loading for a bundle, add `"lazy": true` on the same level as `"bundlename"` in the miniferAppSetup.json.

## Run in development

Webpack dev server is used to serve the JS bundle and assets when running in local development. XHR calls will be proxied to the Java backend assumed to be running on localhost:8080.

So that the server knows to look for the JS bundle and assets from the right place, we need to have the client version configured in the Oskari-server `oskari-ext.properties`:

```
oskari.client.version=dist/devapp
```

To start Webpack dev server run `npm start`. The start script in oskari-frontend package.json defaults to the sample application directory but this can be parameterized for custom apps.

When you see "Compiled successfully." in the terminal, you can open the app in the browser at `localhost:8081`.

The dev server has automatic reloading enabled when you save changes to JS code and hot reloading for S/CSS without need for full browser reload.

## Build for production

To build minifed JS and assets run: `npm run build`.

This will produce optimized files for production under `dist/devapp/servlet/`. The build script in oskari-frontend package.json again defaults to the sample application directory. It also defaults to a version named `devapp`. Both the app and version number can be parameterized for custom apps.

Note: The version number given for the build command needs to match the client version (`oskari.client.version`) in Oskari-server `oskari-ext.properties`.

Special case: If on your production server your application index.jsp location is mapped to something else than the root (eg. `http://yourdomain.com/my-oskari-app/`), but the assets are mapped relative to the root (eg. `http://yourdomain.com/Oskari/dist/...`), you need to add the build parameter `--env.absolutePublicPath=true` like this:

    npm run build -- --env.absolutePublicPath=true

### Customized application icons (optional)

It's possible to override any icon in `oskari-frontend/resources/icons` with app-specific icons. To add icons for your application or to override icons: include a folder named `icons` under the application folder (for example in sample app `applications/sample/servlet/icons`). To replace an icon provide a png-file with the same name as in `oskari-frontend/resources/icons`. For maximum compatibility the pixel size for overridden icon should match the original. Any png-files in the app-specific icons folder will be included in the sprite that is generated so this can be used to add icons for app-specific bundles.

After running the production build it's possible to create a customized set of icons for the application by running a command `npm run sprite -- [version]:[application path]` like

    npm run sprite -- 1.49.0:applications/sample

Note! Requires (GraphicsMagick)[http://www.graphicsmagick.org/] to be installed on the server and the "gm" command to be usable on the cmd/bash.\
Note! You must first run a production build for the application to create the corresponding dist-folder. With the example command the sprite will be generated under the `dist\1.49.0\servlet` folder as `icons.png` and `icons.css`.\
Note! To use the customized icons set your HTML (JSP) on the oskari-server need to link the icons.css under the application folder (default JSP links it from under oskari-frontend/resources/icons.css).

### How to setup custom frontend

See https://github.com/nls-oskari/pti-frontend for an example how to separate app-specific functionality and use oskari-frontend and oskari-frontend-contrib.

### FAQ

#### "Out of memory" error when running Webpack

If you get an error when running the build like  "FATAL ERROR: Committing semi space failed. Allocation failed - process out of memory" or "FATAL ERROR: CALL_AND_RETRY_LAST Allocation failed - JavaScript heap out of memory" you need to configure some more memory for the node-process.

In linux you can use:

    export NODE_OPTIONS=--max_old_space_size=4096
    npm run build -- --env.appdef=1.49.0:applications/sample/

Or in Windows:

    set NODE_OPTIONS=--max_old_space_size=4096 && npm run build -- --env.appdef=1.49.0:applications/sample/

#### Production build "freezes"

CPU usage of the computer shows nothing is happening, but the bash/cmd is still executing the build command. Try setting "parallel" to false on UglifyJsPlugin configuration in webpack.config.js:

    new UglifyJsPlugin({
        sourceMap: true,
        parallel: false
    })

# Reporting issues

All Oskari-related issues should be reported here: https://github.com/oskariorg/oskari-docs/issues

# Contributing

Please read the [contribution guidelines](http://oskari.org/documentation/development/how-to-contribute) before contributing pull requests to the Oskari project.

## Copyright and license

Copyright 2014 - present NLS under dual license MIT (included LICENSE.md) and [EUPL v1.1](https://joinup.ec.europa.eu/software/page/eupl/licence-eupl)
(any language version applies, English version is included in https://github.com/oskariorg/oskari-docs/blob/master/documents/LICENSE-EUPL.pdf).
