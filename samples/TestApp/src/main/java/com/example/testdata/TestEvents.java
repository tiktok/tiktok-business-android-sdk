/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.testdata;

import java.util.ArrayList;
import java.util.HashMap;

public class TestEvents {

    public static String[] getAllEvents() {
        ArrayList<String> events = new ArrayList<>();
        for(String evnt : TTEventProperties.keySet()) {
            events.add(evnt);
        }
        return events.toArray(new String[events.size()]);
    }

    public static HashMap<String, String[]> TTEventProperties = new HashMap<>();

    static {
        TTEventProperties.put("LaunchAPP", new String[]{});
        TTEventProperties.put("InstallApp", new String[]{});
        TTEventProperties.put("2Dretention", new String[]{});
        TTEventProperties.put("AddPaymentInfo", new String[]{
                "success",
        });
        TTEventProperties.put("AddToCart", new String[]{
                "content_type",
                "sku_id",
                "description",
                "currency",
                "value",
        });
        TTEventProperties.put("AddToWishlist", new String[]{
                "page_type",
                "content_id",
                "description",
                "currency",
                "value",
        });
        TTEventProperties.put("Checkout", new String[]{
                "description",
                "sku_id",
                "num_items",
                "payment_available",
                "currency",
                "value",
                "game_item_type",
                "game_item_id",
                "room_type",
                "currency",
                "value",
                "location",
                "checkin_date",
                "checkout_date",
                "number_of_rooms",
                "number_of_nights",
        });
        TTEventProperties.put("CompleteTutorial", new String[]{});
        TTEventProperties.put("ViewContent", new String[]{
                "page_type",
                "sku_id",
                "description",
                "currency",
                "value",
                "search_string",
                "room_type",
                "location",
                "checkin_date",
                "checkout_date",
                "number_of_rooms",
                "number_of_nights",
                "outbound_origination_city",
                "outbound_destination_city",
                "return_origination_city",
                "return_destination_city",
                "class",
                "number_of_passenger",
        });
        TTEventProperties.put("CreateGroup", new String[]{
                "group_name",
                "group_logo",
                "group_description",
                "group_type",
                "group_id",
        });
        TTEventProperties.put("CreateRole", new String[]{
                "role_type",
        });
        TTEventProperties.put("GenerateLead", new String[]{});
        TTEventProperties.put("InAppADClick", new String[]{
                "ad_type",
        });
        TTEventProperties.put("InAppADImpr", new String[]{
                "ad_type",
        });
        TTEventProperties.put("JoinGroup", new String[]{
                "level_number",
        });
        TTEventProperties.put("AchieveLevel", new String[]{
                "level_number",
                "score",
        });
        TTEventProperties.put("LoanApplication", new String[]{
                "loan_type",
                "application_id",
        });
        TTEventProperties.put("LoanApproval", new String[]{
                "value",
        });
        TTEventProperties.put("LoanDisbursal", new String[]{
                "value",
        });
        // TODO remove LOGIN event? If not, determine the ordering.
        TTEventProperties.put("Login", new String[]{});
        TTEventProperties.put("Purchase", new String[]{
                "page_type",
                "sku_id",
                "description",
                "num_items",
                "coupon_used",
                "currency",
                "value",
                "group_type",
                "game_item_id",
                "room_type",
                "location",
                "checkin_date",
                "checkout_date",
                "number_of_rooms",
                "number_of_nights",
                "outbound_origination_city",
                "outbound_destination_city",
                "return_origination_city",
                "return_destination_city",
                "class",
                "number_of_passenger",
                "service_type",
                "service_id",
        });
        TTEventProperties.put("Rate", new String[]{
                "page_type",
                "sku_id",
                "content",
                "rating_value",
                "max_rating_value",
                "rate",
        });
        TTEventProperties.put("Registration", new String[]{
                "registration_method",
        });
        TTEventProperties.put("Search", new String[]{
                "search_string",
                "checkin_date",
                "checkout_date",
                "number_of_rooms",
                "number_of_nights",
                "origination_city",
                "destination_city",
                "departure_date",
                "return_date",
                "class",
                "number_of_passenger",
        });
        TTEventProperties.put("SpendCredits", new String[]{
                "game_item_type",
                "game_item_id",
                "level_number",
        });
        TTEventProperties.put("StartTrial", new String[]{
                "order_id",
                "currency",
        });
        TTEventProperties.put("Subscribe", new String[]{
                "order_id",
                "currency",
        });
        TTEventProperties.put("Share", new String[]{
                "content_type",
                "content_id",
                "shared_destination",
        });
        TTEventProperties.put("Contact", new String[]{});
        TTEventProperties.put("UnlockAchievement", new String[]{
                "description",
                "achievement_type",
        });
    }
}

