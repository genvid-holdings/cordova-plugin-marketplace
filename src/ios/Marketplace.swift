/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

import Foundation
import MarketplaceKit
import OSLog

@objc(Marketplace)
class Marketplace :CDVPlugin {

    @available(iOS 13.0.0, *)
    func getMarketplace() async throws -> String {
        let logger = Logger();
        logger.info("detecting marketplace");
        if #available(iOS 17.4, *) {
            logger.info("Use marketplace");
            var current: AppDistributor;
            do {
                current = try await AppDistributor.current;
            } catch {
                logger.error("Exception: \(error)");
                current = AppDistributor.appStore;
            }
            logger.info("Select current marketplace");
            switch(current) {
            case .testFlight:
                logger.info("It's TestFlight");
                return "TestFlight";
            case .appStore:
                logger.info("It's AppStore");
                return "AppStore";
            case .other:
                logger.info("It's Other");
                return "Other";
            case .marketplace(let marketPlace):
                logger.info("It's marketplace \(marketPlace)");
                return marketPlace;
            default:
                if #available(iOS 17.5, *) {
                    logger.info("Use marketplace 2");
                    switch(current) {
                    case .web:
                        logger.info("It's web");
                        return "web";
                    default:
                        logger.info("It's unknown 2");
                        return "";
                    }
                }
                logger.info("It's unknown");
                return "";
            }
        } else {
            logger.info("No marketplace");
            // Only safe default
            return "AppStore";
        }
    }
    
    @objc(getInfo:)
    func getInfo(command: CDVInvokedUrlCommand)
    {
        if #available(iOS 13.0.0, *) {
            Task {
                let logger = Logger();
                var pluginResult: CDVPluginResult;
                do {
                    let result = try await self.getMarketplace();
                    logger.info("Marketplace is \(result)");
                    pluginResult = CDVPluginResult.init(status: CDVCommandStatus_OK,
                                                  messageAs:["name": result]);
                } catch {
                    logger.error("Failure retrieving marketplace: \(error)");
                    pluginResult = CDVPluginResult.init(status: CDVCommandStatus_ERROR, messageAs: "Error: \(error)");
                }
                self.commandDelegate.send(pluginResult, callbackId: command.callbackId);
            }
        } else {
            let pluginResult = CDVPluginResult.init(status: CDVCommandStatus_ERROR, messageAs: "Unavailable");
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId);
        }
    }
}
