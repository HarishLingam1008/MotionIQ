package com.example.data.model

data class FoodRecommendation(
    val id: String,
    val name: String,
    val category: String, // "Breakfast", "Mid-Morning", "Lunch", "Evening Snack", "Dinner", "Hydration"
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val benefits: String,
    val servingSize: String = "1 serving",
    val whyUseful: String = benefits,
    val affordableAlternative: String = "Vegetable Upma / Dal Khichdi",
    val foodGroup: String = "Balanced Meal", // "Vegetables", "Protein Sources", "Carbohydrates", "Fruits", "Hydration"
    val priceInr: Int = 30,
    val isVeg: Boolean = true
)

data class FoodNutritionLookupItem(
    val id: String,
    val name: String,
    val servingSize: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val category: String = "General", // "Vegetables", "Protein Sources", "Carbohydrates", "Fruits", "Dairy & Nuts"
    val isVeg: Boolean = true
)

data class DailyAffordableMealPlan(
    val breakfast: FoodRecommendation,
    val lunch: FoodRecommendation,
    val eveningSnack: FoodRecommendation,
    val dinner: FoodRecommendation,
    val midMorningSnack: FoodRecommendation? = null,
    val totalCalories: Int,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val estimatedCostInr: Int = 110,
    val targetCalories: Int = 2000,
    val fitnessGoal: String = "Weight Loss",
    val featuredVegetables: List<String> = listOf("Carrot", "Spinach (Palak)", "Toor / Moong Dal", "Bottle Gourd (Lauki)"),
    val goalFeedback: String = "Balanced caloric distribution meeting daily goals."
)

object FoodDataCatalog {

    val recommendedFoods = listOf(
        // ==================== BREAKFAST ====================
        FoodRecommendation(
            id = "b1",
            name = "2 Idlis + Sambar + 1 Boiled Egg",
            category = "Breakfast",
            servingSize = "2 idlis (100g) + 1 cup sambar + 1 egg",
            calories = 310,
            proteinG = 14.5,
            carbsG = 42.0,
            fatG = 6.0,
            benefits = "Affordable, filling and provides carbohydrates + high quality protein for sustained energy.",
            whyUseful = "Affordable, filling, fermented for digestion, and gives sustained morning energy.",
            affordableAlternative = "Vegetable Upma + 1 Boiled Egg",
            foodGroup = "Carbohydrates & Protein",
            priceInr = 35,
            isVeg = false
        ),
        FoodRecommendation(
            id = "b2",
            name = "Steamed Idli (3 pcs) with Drumstick Sambar",
            category = "Breakfast",
            servingSize = "3 idlis + 1 bowl drumstick sambar",
            calories = 240,
            proteinG = 9.0,
            carbsG = 46.0,
            fatG = 2.0,
            benefits = "Steamed, low fat, easy to digest, and rich in toor dal plant protein.",
            whyUseful = "Easily digestible fermented meal rich in lentils, fiber, and drumstick micronutrients.",
            affordableAlternative = "Ragi Idli with Tomato Chutney",
            foodGroup = "Carbohydrates",
            priceInr = 30,
            isVeg = true
        ),
        FoodRecommendation(
            id = "b3",
            name = "Poha with Roasted Peanuts & Veggies",
            category = "Breakfast",
            servingSize = "1 medium bowl (150g)",
            calories = 260,
            proteinG = 7.5,
            carbsG = 42.0,
            fatG = 7.0,
            benefits = "Flattened rice rich in iron, boosted with healthy peanut fats and crunchy onions.",
            whyUseful = "Quick, cheap, light on the stomach, and provides quick morning energy.",
            affordableAlternative = "Vegetable Rava Upma with Peanuts",
            foodGroup = "Carbohydrates",
            priceInr = 20,
            isVeg = true
        ),
        FoodRecommendation(
            id = "b4",
            name = "Moong Dal Chilla with Mint Chutney",
            category = "Breakfast",
            servingSize = "2 medium chillas (120g)",
            calories = 230,
            proteinG = 13.0,
            carbsG = 30.0,
            fatG = 4.5,
            benefits = "High-protein vegetarian crepes made from yellow moong lentils.",
            whyUseful = "Outstanding plant protein, low glycemic index, and keeps you satiated for hours.",
            affordableAlternative = "Besan (Gram Flour) Chilla with Onions",
            foodGroup = "Protein Sources",
            priceInr = 25,
            isVeg = true
        ),
        FoodRecommendation(
            id = "b5",
            name = "Vegetable Rava Upma with 1 Boiled Egg",
            category = "Breakfast",
            servingSize = "1 bowl upma (150g) + 1 egg",
            calories = 280,
            proteinG = 11.0,
            carbsG = 38.0,
            fatG = 8.5,
            benefits = "Semolina loaded with carrots, green peas, curry leaves, and complete egg protein.",
            whyUseful = "Nutritious, highly budget-friendly, and fuels daily steps and focus.",
            affordableAlternative = "Oats Upma with Roasted Gram",
            foodGroup = "Carbohydrates & Protein",
            priceInr = 25,
            isVeg = false
        ),
        FoodRecommendation(
            id = "b6",
            name = "2 Whole Wheat Chapatis with Boiled Egg Bhurji",
            category = "Breakfast",
            servingSize = "2 chapatis + 2 egg bhurji",
            calories = 340,
            proteinG = 18.0,
            carbsG = 38.0,
            fatG = 12.0,
            benefits = "Whole grain complex carbs paired with spiced egg scramble for muscle strength.",
            whyUseful = "High satiety, rich in B-vitamins, and prevents mid-day fatigue.",
            affordableAlternative = "Chapatis with Sprouted Moong Sabzi",
            foodGroup = "Protein Sources",
            priceInr = 30,
            isVeg = false
        ),
        FoodRecommendation(
            id = "b7",
            name = "Ragi Porridge / Malt with Milk & Jaggery",
            category = "Breakfast",
            servingSize = "1 large glass (250ml)",
            calories = 190,
            proteinG = 6.0,
            carbsG = 36.0,
            fatG = 2.5,
            benefits = "Finger millet superfood loaded with natural calcium, iron, and slow-burning carbs.",
            whyUseful = "Strengthens bones, cools the body, and costs less than ₹15 per serving.",
            affordableAlternative = "Oats Porridge with Milk",
            foodGroup = "Carbohydrates",
            priceInr = 15,
            isVeg = true
        ),
        FoodRecommendation(
            id = "b8",
            name = "Boiled Green Gram (Sprouted Moong) Chaat",
            category = "Breakfast",
            servingSize = "1 bowl (150g)",
            calories = 180,
            proteinG = 11.5,
            carbsG = 28.0,
            fatG = 1.5,
            benefits = "Sprouted green gram with lemon, onions, tomatoes, and chaat masala.",
            whyUseful = "Live enzymes, high fiber, high bioavailable iron, and zero cooking oil.",
            affordableAlternative = "Boiled Black Chickpeas (Kala Chana) Chaat",
            foodGroup = "Protein Sources",
            priceInr = 15,
            isVeg = true
        ),

        // ==================== MID-MORNING ====================
        FoodRecommendation(
            id = "m1",
            name = "1 Medium Banana + Water",
            category = "Mid-Morning",
            servingSize = "1 medium banana (120g)",
            calories = 105,
            proteinG = 1.3,
            carbsG = 27.0,
            fatG = 0.3,
            benefits = "Instant potassium and fast natural energy for muscle contractions and walking stamina.",
            whyUseful = "Super cheap (₹5-7), universally available, and prevents muscle cramps.",
            affordableAlternative = "1 Fresh Guava with rock salt",
            foodGroup = "Fruits",
            priceInr = 7,
            isVeg = true
        ),
        FoodRecommendation(
            id = "m2",
            name = "Fresh Guava with Chaat Masala",
            category = "Mid-Morning",
            servingSize = "1 whole guava (150g)",
            calories = 75,
            proteinG = 3.0,
            carbsG = 16.0,
            fatG = 1.0,
            benefits = "Contains 4x more Vitamin C than oranges, extremely high in dietary fiber.",
            whyUseful = "Supports immunity, controls blood sugar spikes, and is very inexpensive.",
            affordableAlternative = "1 Orange / Sweet Lime",
            foodGroup = "Fruits",
            priceInr = 10,
            isVeg = true
        ),
        FoodRecommendation(
            id = "m3",
            name = "1 Glass Spiced Buttermilk (Chaas)",
            category = "Mid-Morning",
            servingSize = "1 glass (250ml)",
            calories = 50,
            proteinG = 3.0,
            carbsG = 4.0,
            fatG = 2.0,
            benefits = "Probiotic churned curd spiced with cumin, mint, and rock salt.",
            whyUseful = "Hydrates, aids digestion, cools internal core temperature during daily activity.",
            affordableAlternative = "Fresh Lemon Rock Salt Water (Nimbu Pani)",
            foodGroup = "Hydration",
            priceInr = 10,
            isVeg = true
        ),
        FoodRecommendation(
            id = "m4",
            name = "Papaya Cubes with Lemon",
            category = "Mid-Morning",
            servingSize = "1 cup (150g)",
            calories = 65,
            proteinG = 1.0,
            carbsG = 15.0,
            fatG = 0.2,
            benefits = "Contains papain digestive enzymes that cleanse the gut and reduce inflammation.",
            whyUseful = "Low in calories, gentle on the stomach, and promotes skin health.",
            affordableAlternative = "Watermelon Slices",
            foodGroup = "Fruits",
            priceInr = 15,
            isVeg = true
        ),
        FoodRecommendation(
            id = "m5",
            name = "Handful of Roasted Peanuts & Chana",
            category = "Mid-Morning",
            servingSize = "30g mix",
            calories = 140,
            proteinG = 6.5,
            carbsG = 12.0,
            fatG = 7.0,
            benefits = "The classic Indian poor man's dry fruit mix packed with protein and healthy fats.",
            whyUseful = "Affordable, non-perishable, and gives long-lasting energy.",
            affordableAlternative = "Boiled Kala Chana",
            foodGroup = "Protein Sources",
            priceInr = 10,
            isVeg = true
        ),

        // ==================== LUNCH ====================
        FoodRecommendation(
            id = "l1",
            name = "Steamed Rice (100g) + Toor Dal + Cabbage Poriyal + Curd",
            category = "Lunch",
            servingSize = "1 cup rice + 1 cup dal + 1 cup sabzi + 1/2 cup curd",
            calories = 420,
            proteinG = 15.0,
            carbsG = 72.0,
            fatG = 6.5,
            benefits = "Balanced Indian thali with complete amino acids from lentils, gut probiotics, and vegetable fiber.",
            whyUseful = "Wholesome, traditional, very budget-friendly, and covers all essential macronutrients.",
            affordableAlternative = "Rice + Sambar + Bottle Gourd (Lauki) Sabzi",
            foodGroup = "Balanced Meal",
            priceInr = 35,
            isVeg = true
        ),
        FoodRecommendation(
            id = "l2",
            name = "2 Whole Wheat Chapatis + Dal Tadka + Spinach Sabzi",
            category = "Lunch",
            servingSize = "2 phulkas + 1 cup dal + 1 cup palak",
            calories = 360,
            proteinG = 14.0,
            carbsG = 58.0,
            fatG = 6.0,
            benefits = "Complex whole wheat carbs, iron from spinach, and rich plant protein from dal.",
            whyUseful = "Great for weight control and steady glucose release without post-lunch sleepiness.",
            affordableAlternative = "Chapatis + Ridge Gourd (Torai) Sabzi + Dal",
            foodGroup = "Vegetables & Protein",
            priceInr = 30,
            isVeg = true
        ),
        FoodRecommendation(
            id = "l3",
            name = "Kala Chana (Black Chickpea) Curry + 2 Phulkas + Cucumber Salad",
            category = "Lunch",
            servingSize = "1 bowl chana curry + 2 phulkas + salad",
            calories = 390,
            proteinG = 16.5,
            carbsG = 62.0,
            fatG = 6.5,
            benefits = "Black chickpeas are loaded with slow-release carbs, dietary fiber, and bioavailable iron.",
            whyUseful = "Exceptional athletic fuel, highly satiating, and very low cost per gram of protein.",
            affordableAlternative = "Rajma Masala with Steamed Rice",
            foodGroup = "Protein Sources",
            priceInr = 30,
            isVeg = true
        ),
        FoodRecommendation(
            id = "l4",
            name = "Egg Curry (2 Eggs) + Steamed Rice (100g) + Tomato Salad",
            category = "Lunch",
            servingSize = "2 eggs in curry + 1 cup rice + salad",
            calories = 430,
            proteinG = 19.0,
            carbsG = 52.0,
            fatG = 14.0,
            benefits = "High-quality animal protein with choline and lutein in an onion-tomato gravy.",
            whyUseful = "Affordable complete protein meal for muscle repair and recovery after walking.",
            affordableAlternative = "Soya Chunks Curry + Steamed Rice",
            foodGroup = "Protein Sources",
            priceInr = 40,
            isVeg = false
        ),
        FoodRecommendation(
            id = "l5",
            name = "Soya Chunks Curry + 2 Chapatis + Beetroot Salad",
            category = "Lunch",
            servingSize = "1 cup soya curry + 2 chapatis + beetroot",
            calories = 380,
            proteinG = 24.0,
            carbsG = 52.0,
            fatG = 5.0,
            benefits = "The ultimate affordable vegetarian protein powerhouse with 52% protein density.",
            whyUseful = "Highest protein per rupee in India, lean, and helps in fat loss and muscle retention.",
            affordableAlternative = "Paneer Bhurji with Chapatis",
            foodGroup = "Protein Sources",
            priceInr = 25,
            isVeg = true
        ),
        FoodRecommendation(
            id = "l6",
            name = "Curd Rice with Pomegranate + Boiled Beans Poriyal",
            category = "Lunch",
            servingSize = "1 cup curd rice + 1 cup green beans",
            calories = 310,
            proteinG = 9.0,
            carbsG = 52.0,
            fatG = 6.0,
            benefits = "Cooling probiotic meal that soothes the digestive tract and provides antioxidant polyphenols.",
            whyUseful = "Ideal for hot days and recovery after extensive outdoor walking steps.",
            affordableAlternative = "Khichdi with Fresh Dahi",
            foodGroup = "Balanced Meal",
            priceInr = 30,
            isVeg = true
        ),
        FoodRecommendation(
            id = "l7",
            name = "Drumstick & Vegetable Sambar + Brown/White Rice",
            category = "Lunch",
            servingSize = "1.5 cup sambar + 1 cup rice",
            calories = 350,
            proteinG = 11.0,
            carbsG = 64.0,
            fatG = 4.0,
            benefits = "Traditional South Indian lentil stew packed with drumstick, carrots, pumpkin, and tamarind.",
            whyUseful = "Rich in iron, potassium, and antioxidants, very light on digestion.",
            affordableAlternative = "Dal Khichdi with Mixed Vegetables",
            foodGroup = "Vegetables & Carbs",
            priceInr = 30,
            isVeg = true
        ),

        // ==================== EVENING SNACK ====================
        FoodRecommendation(
            id = "s1",
            name = "Roasted Chana (Bengal Gram) - 1 Bowl",
            category = "Evening Snack",
            servingSize = "40g roasted chana",
            calories = 140,
            proteinG = 8.0,
            carbsG = 22.0,
            fatG = 2.2,
            benefits = "High-protein crunchy snack with zero added oil and low glycemic index.",
            whyUseful = "Costs ~₹10, highly portable, curbs evening hunger without excess calories.",
            affordableAlternative = "Boiled Peanut Chaat with Lemon",
            foodGroup = "Protein Sources",
            priceInr = 10,
            isVeg = true
        ),
        FoodRecommendation(
            id = "s2",
            name = "Boiled Peanut Chaat (with Onion & Tomato)",
            category = "Evening Snack",
            servingSize = "50g boiled peanuts + salad",
            calories = 170,
            proteinG = 8.5,
            carbsG = 12.0,
            fatG = 9.5,
            benefits = "Heart-healthy monounsaturated fats, protein, and dietary fiber tossed with lemon.",
            whyUseful = "Keeps you full till dinner, preventing unhealthy fried snack cravings.",
            affordableAlternative = "Roasted Peanuts with Murmura Bhel",
            foodGroup = "Protein Sources",
            priceInr = 15,
            isVeg = true
        ),
        FoodRecommendation(
            id = "s3",
            name = "1 Boiled Egg with Pinch of Black Pepper",
            category = "Evening Snack",
            servingSize = "1 large egg (50g)",
            calories = 74,
            proteinG = 6.3,
            carbsG = 0.6,
            fatG = 5.0,
            benefits = "Pure bioavailable protein and healthy fats for immediate post-walk muscle nourishment.",
            whyUseful = "Costs only ~₹6-7, practically zero carbs, perfect for weight management.",
            affordableAlternative = "1 Glass Sattu Drink",
            foodGroup = "Protein Sources",
            priceInr = 7,
            isVeg = false
        ),
        FoodRecommendation(
            id = "s4",
            name = "Bihari Sattu Drink (Roasted Gram Flour)",
            category = "Evening Snack",
            servingSize = "2 tbsp sattu in 250ml water + lemon + cumin",
            calories = 120,
            proteinG = 7.0,
            carbsG = 18.0,
            fatG = 1.8,
            benefits = "India's natural desi protein shake. Refreshing, high-fiber, and deeply hydrating.",
            whyUseful = "Instant protein at ₹10 per glass, cools the gut and prevents fatigue.",
            affordableAlternative = "Masala Chaas with Roasted Cumin",
            foodGroup = "Protein Sources",
            priceInr = 10,
            isVeg = true
        ),
        FoodRecommendation(
            id = "s5",
            name = "Murmura (Puffed Rice) Bhel with Roasted Chana & Peanuts",
            category = "Evening Snack",
            servingSize = "1 medium bowl (100g)",
            calories = 130,
            proteinG = 4.5,
            carbsG = 24.0,
            fatG = 2.5,
            benefits = "Crisp, light, oil-free snack mixed with diced onions, tomatoes, and coriander.",
            whyUseful = "Low in calories, satisfyingly crunchy, and very budget-friendly.",
            affordableAlternative = "Steamed Sweet Corn with Lemon",
            foodGroup = "Carbohydrates",
            priceInr = 12,
            isVeg = true
        ),
        FoodRecommendation(
            id = "s6",
            name = "Steamed Sweet Potato (Shakarkandi) Chaat",
            category = "Evening Snack",
            servingSize = "100g steamed sweet potato",
            calories = 90,
            proteinG = 1.8,
            carbsG = 20.5,
            fatG = 0.2,
            benefits = "Rich in beta-carotene (vitamin A), slow-release complex carbs, and potassium.",
            whyUseful = "Refuels glycogen stores after brisk walking without spiking insulin.",
            affordableAlternative = "1 Medium Banana",
            foodGroup = "Carbohydrates",
            priceInr = 15,
            isVeg = true
        ),

        // ==================== DINNER ====================
        FoodRecommendation(
            id = "d1",
            name = "2 Phulkas + Lauki (Bottle Gourd) Sabzi + Yellow Moong Dal",
            category = "Dinner",
            servingSize = "2 phulkas + 1 cup lauki + 1/2 cup dal",
            calories = 290,
            proteinG = 11.0,
            carbsG = 48.0,
            fatG = 4.5,
            benefits = "Light, high-water content vegetable curry with easy-to-digest yellow lentils.",
            whyUseful = "Prevents night bloating, low calorie, and aids restful sleep and metabolic recovery.",
            affordableAlternative = "Phulkas + Ridge Gourd (Torai) Sabzi + Dal",
            foodGroup = "Vegetables & Protein",
            priceInr = 25,
            isVeg = true
        ),
        FoodRecommendation(
            id = "d2",
            name = "Moong Dal & Spinach (Palak) Khichdi with Dahi",
            category = "Dinner",
            servingSize = "1 bowl khichdi (200g) + 1/2 cup curd",
            calories = 310,
            proteinG = 13.0,
            carbsG = 50.0,
            fatG = 5.0,
            benefits = "Ayurvedic one-pot complete nutrition with iron from spinach and soothing lentils.",
            whyUseful = "Warm, comforting, extremely gentle on digestion, and cooks in 15 minutes.",
            affordableAlternative = "Oats & Vegetable Khichdi",
            foodGroup = "Balanced Meal",
            priceInr = 25,
            isVeg = true
        ),
        FoodRecommendation(
            id = "d3",
            name = "2 Phulkas + Soya Bhurji (Minced Soya Granules)",
            category = "Dinner",
            servingSize = "2 phulkas + 1 cup soya bhurji",
            calories = 320,
            proteinG = 22.0,
            carbsG = 42.0,
            fatG = 5.5,
            benefits = "High-protein low-fat dinner that supports overnight muscle protein synthesis.",
            whyUseful = "Cost-effective vegetarian lean dinner under 350 kcal.",
            affordableAlternative = "2 Boiled Eggs Salad with Phulkas",
            foodGroup = "Protein Sources",
            priceInr = 25,
            isVeg = true
        ),
        FoodRecommendation(
            id = "d4",
            name = "Clear Vegetable & Lentil Soup with 1 Phulka",
            category = "Dinner",
            servingSize = "1 large bowl soup + 1 chapati",
            calories = 190,
            proteinG = 8.5,
            carbsG = 32.0,
            fatG = 2.5,
            benefits = "Hydrating vegetable broth with carrot, cabbage, beans, and yellow lentils.",
            whyUseful = "Ultra-light dinner ideal for weight loss and deep sleep.",
            affordableAlternative = "Tomato & Masoor Dal Soup",
            foodGroup = "Vegetables",
            priceInr = 20,
            isVeg = true
        ),
        FoodRecommendation(
            id = "d5",
            name = "2 Boiled Eggs + Cucumber & Tomato Salad + 1 Phulka",
            category = "Dinner",
            servingSize = "2 eggs + large salad + 1 phulka",
            calories = 260,
            proteinG = 16.5,
            carbsG = 22.0,
            fatG = 11.0,
            benefits = "Low carb, high protein dinner providing all essential amino acids with hydration.",
            whyUseful = "Light, keeps insulin low overnight, and supports steady fat burning.",
            affordableAlternative = "100g Paneer & Capsicum Stir-fry",
            foodGroup = "Protein Sources",
            priceInr = 25,
            isVeg = false
        ),
        FoodRecommendation(
            id = "d6",
            name = "2 Phulkas + Pumpkin (Kaddu) Sabzi + Curd",
            category = "Dinner",
            servingSize = "2 phulkas + 1 cup kaddu + 1/2 cup curd",
            calories = 270,
            proteinG = 9.0,
            carbsG = 46.0,
            fatG = 4.5,
            benefits = "Pumpkin is rich in antioxidants, beta-carotene, and magnesium for muscle relaxation.",
            whyUseful = "Naturally sweet, low in calories, and supports nighttime melatonin production.",
            affordableAlternative = "Phulkas + Bhindi (Ladies Finger) Sabzi",
            foodGroup = "Vegetables",
            priceInr = 25,
            isVeg = true
        )
    )

    // ==================== 50+ FOOD CALORIE & NUTRITION LOOKUP DATABASE ====================
    val foodLookupCatalog = listOf(
        // Carbohydrates & Staples
        FoodNutritionLookupItem("f1", "1 Banana (Medium)", "1 medium (120g)", 105, 1.3, 27.0, 0.3, 3.1, "Fruits", true),
        FoodNutritionLookupItem("f2", "1 Boiled Egg", "1 large (50g)", 74, 6.3, 0.6, 5.0, 0.0, "Protein Sources", false),
        FoodNutritionLookupItem("f3", "1 Egg Omelette (1 Egg)", "1 egg (with 1 tsp oil)", 95, 7.0, 1.0, 7.0, 0.0, "Protein Sources", false),
        FoodNutritionLookupItem("f4", "100g Cooked Rice (White)", "100g cooked", 130, 2.7, 28.2, 0.3, 0.4, "Carbohydrates", true),
        FoodNutritionLookupItem("f5", "100g Cooked Brown Rice", "100g cooked", 112, 2.6, 23.5, 0.9, 1.8, "Carbohydrates", true),
        FoodNutritionLookupItem("f6", "1 Steamed Idli", "1 medium idli (45g)", 60, 2.0, 12.0, 0.2, 0.8, "Carbohydrates", true),
        FoodNutritionLookupItem("f7", "1 Plain Dosa", "1 medium (60g)", 135, 3.0, 22.0, 4.0, 1.0, "Carbohydrates", true),
        FoodNutritionLookupItem("f8", "1 Chapati / Phulka (Wheat)", "1 medium phulka (35g)", 85, 3.0, 18.0, 0.5, 2.5, "Carbohydrates", true),
        FoodNutritionLookupItem("f9", "1 Cup Cooked Dal (Toor/Moong)", "1 cup (150g)", 150, 9.0, 22.0, 3.0, 5.0, "Protein Sources", true),
        FoodNutritionLookupItem("f10", "100g Boiled Potatoes", "100g boiled", 87, 1.9, 20.1, 0.1, 2.2, "Carbohydrates", true),
        FoodNutritionLookupItem("f11", "100g Steamed Sweet Potato", "100g steamed", 86, 1.6, 20.1, 0.1, 3.0, "Carbohydrates", true),
        FoodNutritionLookupItem("f12", "1 Cup Cooked Poha", "1 cup (150g)", 180, 4.0, 35.0, 3.0, 2.0, "Carbohydrates", true),
        FoodNutritionLookupItem("f13", "1 Cup Cooked Upma", "1 cup (150g)", 200, 4.5, 34.0, 5.5, 2.2, "Carbohydrates", true),
        FoodNutritionLookupItem("f14", "1 Cup Cooked Oats", "1 cup (150g)", 150, 5.0, 27.0, 2.5, 4.0, "Carbohydrates", true),
        FoodNutritionLookupItem("f15", "1 Glass Ragi Malt / Porridge", "250ml", 120, 3.5, 25.0, 1.0, 3.5, "Carbohydrates", true),

        // Vegetables
        FoodNutritionLookupItem("f16", "100g Carrot (Raw/Cooked)", "100g", 41, 0.9, 9.6, 0.2, 2.8, "Vegetables", true),
        FoodNutritionLookupItem("f17", "100g Tomato (Fresh)", "100g", 18, 0.9, 3.9, 0.2, 1.2, "Vegetables", true),
        FoodNutritionLookupItem("f18", "100g Cabbage (Raw/Cooked)", "100g", 25, 1.3, 5.8, 0.1, 2.5, "Vegetables", true),
        FoodNutritionLookupItem("f19", "100g Spinach (Palak)", "100g fresh leaves", 23, 2.9, 3.6, 0.4, 2.2, "Vegetables", true),
        FoodNutritionLookupItem("f20", "100g Bottle Gourd (Lauki)", "100g cooked", 14, 0.6, 3.4, 0.1, 1.2, "Vegetables", true),
        FoodNutritionLookupItem("f21", "100g Ridge Gourd (Torai)", "100g cooked", 17, 0.7, 3.4, 0.2, 1.5, "Vegetables", true),
        FoodNutritionLookupItem("f22", "100g Drumstick (Moringa pods)", "100g", 37, 2.1, 8.5, 0.2, 3.2, "Vegetables", true),
        FoodNutritionLookupItem("f23", "100g Brinjal / Eggplant", "100g", 25, 1.0, 5.9, 0.2, 3.0, "Vegetables", true),
        FoodNutritionLookupItem("f24", "100g Ladies Finger (Bhindi)", "100g", 33, 1.9, 7.5, 0.2, 3.2, "Vegetables", true),
        FoodNutritionLookupItem("f25", "100g Pumpkin (Kaddu)", "100g", 26, 1.0, 6.5, 0.1, 1.1, "Vegetables", true),
        FoodNutritionLookupItem("f26", "100g Beetroot", "100g", 43, 1.6, 9.6, 0.2, 2.8, "Vegetables", true),
        FoodNutritionLookupItem("f27", "100g Cucumber (with peel)", "100g fresh", 15, 0.7, 3.6, 0.1, 0.5, "Vegetables", true),
        FoodNutritionLookupItem("f28", "100g Cauliflower", "100g", 25, 1.9, 5.0, 0.3, 2.0, "Vegetables", true),
        FoodNutritionLookupItem("f29", "100g Green Peas (Matar)", "100g", 81, 5.4, 14.5, 0.4, 5.7, "Vegetables", true),
        FoodNutritionLookupItem("f30", "100g French Beans", "100g", 31, 1.8, 7.0, 0.2, 2.7, "Vegetables", true),
        FoodNutritionLookupItem("f31", "100g Snake Gourd (Chichinda)", "100g", 18, 0.6, 3.3, 0.3, 1.0, "Vegetables", true),

        // Protein & Dairy
        FoodNutritionLookupItem("f32", "1 Cup Curd / Dahi (Plain)", "1 cup (150g)", 100, 5.0, 6.0, 4.0, 0.0, "Dairy & Nuts", true),
        FoodNutritionLookupItem("f33", "100g Roasted Peanuts", "100g", 567, 25.8, 16.1, 49.2, 8.5, "Dairy & Nuts", true),
        FoodNutritionLookupItem("f34", "100g Boiled Peanuts", "100g", 318, 13.5, 12.0, 22.0, 4.0, "Dairy & Nuts", true),
        FoodNutritionLookupItem("f35", "100g Green Gram / Moong (Cooked)", "100g", 105, 7.0, 19.0, 0.4, 7.6, "Protein Sources", true),
        FoodNutritionLookupItem("f36", "100g Black Chickpeas (Kala Chana)", "100g cooked", 164, 8.9, 27.4, 2.6, 7.6, "Protein Sources", true),
        FoodNutritionLookupItem("f37", "100g White Chickpeas (Kabuli Chana)", "100g cooked", 164, 8.9, 27.4, 2.6, 7.6, "Protein Sources", true),
        FoodNutritionLookupItem("f38", "100g Soya Chunks (Cooked)", "100g boiled", 145, 18.0, 10.0, 1.0, 6.0, "Protein Sources", true),
        FoodNutritionLookupItem("f39", "100g Fresh Paneer", "100g", 265, 18.3, 3.4, 20.8, 0.0, "Protein Sources", true),
        FoodNutritionLookupItem("f40", "1 Glass Cow Milk (200ml)", "200ml", 120, 6.4, 9.6, 6.0, 0.0, "Dairy & Nuts", true),
        FoodNutritionLookupItem("f41", "1 Cup Roasted Gram / Sattu (30g)", "30g", 115, 6.8, 17.5, 1.6, 4.2, "Protein Sources", true),
        FoodNutritionLookupItem("f42", "1 Glass Spiced Chaas (Buttermilk)", "250ml", 50, 3.0, 4.0, 2.0, 0.0, "Dairy & Nuts", true),

        // Fruits
        FoodNutritionLookupItem("f43", "1 Medium Guava (Amrood)", "1 medium (150g)", 68, 2.6, 14.3, 1.0, 5.4, "Fruits", true),
        FoodNutritionLookupItem("f44", "100g Fresh Papaya", "100g", 43, 0.5, 10.8, 0.3, 1.7, "Fruits", true),
        FoodNutritionLookupItem("f45", "100g Fresh Watermelon", "100g", 30, 0.6, 7.6, 0.2, 0.4, "Fruits", true),
        FoodNutritionLookupItem("f46", "1 Medium Orange / Mosambi", "1 medium (130g)", 62, 1.2, 15.4, 0.2, 3.1, "Fruits", true),
        FoodNutritionLookupItem("f47", "1 Medium Apple", "1 medium (150g)", 95, 0.5, 25.0, 0.3, 4.4, "Fruits", true)
    )

    fun searchFoodLookup(query: String): List<FoodNutritionLookupItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return foodLookupCatalog
        return foodLookupCatalog.filter {
            it.name.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.servingSize.lowercase().contains(q)
        }
    }

    /**
     * Generates a balanced, affordable 4-meal daily food plan (Breakfast, Lunch, Evening Snack, Dinner)
     * prioritizing common nutritious vegetables like carrot, spinach (palak), and dal.
     * Clicking "Regenerate" cycles through fresh combinations using seed variations.
     */
    fun generateDailyMealPlan(
        seed: Int = 0,
        dietPreference: String = "All",
        targetCalories: Int = 2000,
        fitnessGoal: String = "Weight Loss"
    ): DailyAffordableMealPlan {
        val isVegOnly = dietPreference.equals("Veg", ignoreCase = true) || dietPreference.equals("Vegetarian", ignoreCase = true)

        // Filter and sort items to prioritize meals rich in carrots, spinach, and lentils/dal
        val breakfasts = recommendedFoods.filter { it.category == "Breakfast" && (!isVegOnly || it.isVeg) }
            .sortedByDescending { if (it.name.contains("Dal", true) || it.name.contains("Veg", true) || it.name.contains("Egg", true)) 1 else 0 }
        val lunches = recommendedFoods.filter { it.category == "Lunch" && (!isVegOnly || it.isVeg) }
            .sortedByDescending { if (it.name.contains("Dal", true) || it.name.contains("Palak", true) || it.name.contains("Spinach", true) || it.name.contains("Carrot", true)) 1 else 0 }
        val eveningSnacks = recommendedFoods.filter { it.category == "Evening Snack" && (!isVegOnly || it.isVeg) }
            .sortedByDescending { if (it.name.contains("Chana", true) || it.name.contains("Peanut", true) || it.name.contains("Moong", true)) 1 else 0 }
        val dinners = recommendedFoods.filter { it.category == "Dinner" && (!isVegOnly || it.isVeg) }
            .sortedByDescending { if (it.name.contains("Lauki", true) || it.name.contains("Dal", true) || it.name.contains("Spinach", true) || it.name.contains("Palak", true)) 1 else 0 }
        val midMorningSnacks = recommendedFoods.filter { it.category == "Mid-Morning" && (!isVegOnly || it.isVeg) }

        val b = breakfasts[(seed) % breakfasts.size.coerceAtLeast(1)]
        val l = lunches[(seed + 1) % lunches.size.coerceAtLeast(1)]
        val s = eveningSnacks[(seed + 2) % eveningSnacks.size.coerceAtLeast(1)]
        val d = dinners[(seed + 3) % dinners.size.coerceAtLeast(1)]
        val m = if (midMorningSnacks.isNotEmpty()) midMorningSnacks[(seed + 4) % midMorningSnacks.size] else null

        val totalCal = b.calories + l.calories + s.calories + d.calories
        val totalProt = b.proteinG + l.proteinG + s.proteinG + d.proteinG
        val totalCarb = b.carbsG + l.carbsG + s.carbsG + d.carbsG
        val totalFat = b.fatG + l.fatG + s.fatG + d.fatG
        val totalCost = b.priceInr + l.priceInr + s.priceInr + d.priceInr

        // Collect featured staple vegetables
        val vegSet = mutableSetOf<String>()
        val combinedText = "${b.name} ${b.benefits} ${l.name} ${l.benefits} ${s.name} ${s.benefits} ${d.name} ${d.benefits}".lowercase()
        if (combinedText.contains("spinach") || combinedText.contains("palak")) vegSet.add("Spinach (Palak)")
        if (combinedText.contains("carrot")) vegSet.add("Fresh Carrots")
        if (combinedText.contains("dal") || combinedText.contains("sambar") || combinedText.contains("lentil") || combinedText.contains("moong")) vegSet.add("Toor & Moong Dal")
        if (combinedText.contains("lauki") || combinedText.contains("gourd")) vegSet.add("Bottle Gourd (Lauki)")
        if (combinedText.contains("chana") || combinedText.contains("sprout")) vegSet.add("Sprouted Chana")
        if (combinedText.contains("egg")) vegSet.add("Farm Boiled Eggs")
        if (vegSet.isEmpty()) {
            vegSet.addAll(listOf("Carrots", "Spinach (Palak)", "Yellow Moong Dal"))
        }

        val feedback = when (fitnessGoal.lowercase()) {
            "weight loss", "loss", "lose weight" -> {
                val deficit = targetCalories - totalCal
                if (deficit >= 0) "Optimal for Weight Loss: ~$totalCal kcal plan creates a clean deficit against your $targetCalories kcal target."
                else "Clean low-calorie Indian volume plan: high satiety fiber from spinach, carrots & dal."
            }
            "muscle gain", "gain", "bulk" -> "Muscle Building Plan: ~${totalProt.toInt()}g protein paired with complex carbs and nutrient-dense dal & eggs."
            else -> "Maintenance Plan: Balanced energy balance (~$totalCal kcal) with wholesome vegetables & pulses."
        }

        return DailyAffordableMealPlan(
            breakfast = b,
            lunch = l,
            eveningSnack = s,
            dinner = d,
            midMorningSnack = m,
            totalCalories = totalCal,
            totalProteinG = totalProt,
            totalCarbsG = totalCarb,
            totalFatG = totalFat,
            estimatedCostInr = totalCost,
            targetCalories = targetCalories,
            fitnessGoal = fitnessGoal,
            featuredVegetables = vegSet.toList(),
            goalFeedback = feedback
        )
    }
}
