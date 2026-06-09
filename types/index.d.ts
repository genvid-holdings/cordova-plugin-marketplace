// Type definitions for cordova-plugin-marketplace
// Project: https://github.com/genvid-holdings/cordova-plugin-marketplace
// Definitions by: Microsoft Open Technologies Inc <http://msopentech.com>
//                 Tim Brust <https://github.com/timbru31>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

/**
 * This plugin exposed marketplace/source installation for the app.
 */

interface Marketplace {
    /** Indicates that Cordova initialize successfully. */
    available: boolean;
    name?: string;
}

declare var plugins: {
    marketplace: Marketplace;
};
