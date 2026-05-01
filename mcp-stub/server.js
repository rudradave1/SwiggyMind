/**
 * SwiggyMind — Local MCP Stub Server
 *
 * Mirrors mcp.swiggy.com endpoints locally for development.
 * Android emulator reaches this at: http://10.0.2.2:3000
 * Physical device (same WiFi): http://<your-machine-ip>:3000
 *
 * Usage:
 *   npm install && npm start
 *
 * Then in build.gradle.kts set:
 *   buildConfigField("Boolean", "USE_MCP_BACKEND", "true")
 */

const express = require('express');
const app = express();
app.use(express.json());

// ── Helpers ──────────────────────────────────────────────────────────────────

function ok(id, data) {
  return {
    jsonrpc: '2.0',
    id: id ?? '1',
    result: {
      content: [{ type: 'text', text: JSON.stringify({ success: true, data }) }]
    }
  };
}

function err(id, code, message) {
  return { jsonrpc: '2.0', id: id ?? '1', error: { code, message } };
}

// ── Stub data ─────────────────────────────────────────────────────────────────

const ADDRESSES = [
  { id: 'addr_1', label: 'Home', displayAddress: '42 MG Road, Ahmedabad 380009' },
  { id: 'addr_2', label: 'Work', displayAddress: 'SG Highway, Ahmedabad 380054' }
];

const FOOD_RESTAURANTS = [
  {
    id: 'r_f001', name: 'Biryani Blues', cuisines: ['North Indian', 'Biryani'],
    avgRating: 4.4, sla: { deliveryTime: 28 }, costForTwo: 450, isVeg: false,
    availabilityStatus: 'OPEN', locality: 'Ahmedabad',
    imageUrl: 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=400&q=80',
    tags: ['popular', 'must-try']
  },
  {
    id: 'r_f002', name: 'Patel\'s Thali House', cuisines: ['Gujarati', 'Thali'],
    avgRating: 4.6, sla: { deliveryTime: 22 }, costForTwo: 300, isVeg: true,
    availabilityStatus: 'OPEN', locality: 'Ahmedabad',
    imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80',
    tags: ['veg', 'unlimited', 'family']
  },
  {
    id: 'r_f003', name: 'Wok On Fire', cuisines: ['Chinese', 'Asian'],
    avgRating: 4.2, sla: { deliveryTime: 35 }, costForTwo: 500, isVeg: false,
    availabilityStatus: 'OPEN', locality: 'Ahmedabad',
    imageUrl: 'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=400&q=80',
    tags: ['spicy', 'noodles']
  },
  {
    id: 'r_f004', name: 'Pizza Republic', cuisines: ['Italian', 'Pizza'],
    avgRating: 4.3, sla: { deliveryTime: 40 }, costForTwo: 600, isVeg: false,
    availabilityStatus: 'OPEN', locality: 'Mumbai',
    imageUrl: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&q=80',
    tags: ['pizza', 'cheesy']
  },
  {
    id: 'r_f005', name: 'Dosa Plaza', cuisines: ['South Indian', 'Breakfast'],
    avgRating: 4.5, sla: { deliveryTime: 20 }, costForTwo: 200, isVeg: true,
    availabilityStatus: 'OPEN', locality: 'Bangalore',
    imageUrl: 'https://images.unsplash.com/photo-1630409346824-4f0e7b080087?w=400&q=80',
    tags: ['quick', 'veg', 'light']
  },
  {
    id: 'r_f006', name: 'Burger Singh', cuisines: ['American', 'Burgers'],
    avgRating: 4.1, sla: { deliveryTime: 25 }, costForTwo: 350, isVeg: false,
    availabilityStatus: 'OPEN', locality: 'Mumbai',
    imageUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&q=80',
    tags: ['burgers', 'fast-food']
  },
  {
    id: 'r_f007', name: 'Chole Bhature Wala', cuisines: ['North Indian', 'Street Food'],
    avgRating: 4.7, sla: { deliveryTime: 18 }, costForTwo: 180, isVeg: true,
    availabilityStatus: 'OPEN', locality: 'Ahmedabad',
    imageUrl: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400&q=80',
    tags: ['budget', 'quick', 'spicy']
  },
  {
    id: 'r_f008', name: 'Mainland China', cuisines: ['Chinese', 'Pan-Asian'],
    avgRating: 4.4, sla: { deliveryTime: 45 }, costForTwo: 800, isVeg: false,
    availabilityStatus: 'OPEN', locality: 'Bangalore',
    imageUrl: 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400&q=80',
    tags: ['premium', 'spicy']
  }
];

const INSTAMART_PRODUCTS = [
  {
    id: 'im_001', name: 'Amul Full Cream Milk 1L', category: 'Dairy',
    price: 68, imageUrl: 'https://images.unsplash.com/photo-1563636619-e9143da7973b?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_002', name: 'Farm Fresh Eggs (12 pcs)', category: 'Eggs & Dairy',
    price: 96, imageUrl: 'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_003', name: 'Brown Bread (400g)', category: 'Bakery',
    price: 42, imageUrl: 'https://images.unsplash.com/photo-1549931319-a545dcf3bc73?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_004', name: 'Onions (1kg)', category: 'Vegetables',
    price: 35, imageUrl: 'https://images.unsplash.com/photo-1508747703725-719777637510?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_005', name: 'Tomatoes (500g)', category: 'Vegetables',
    price: 28, imageUrl: 'https://images.unsplash.com/photo-1561136594-7f68413baa99?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_006', name: 'Basmati Rice 5kg', category: 'Rice & Grains',
    price: 380, imageUrl: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_007', name: 'Amul Butter 500g', category: 'Dairy',
    price: 250, imageUrl: 'https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=200&q=80',
    inStock: true
  },
  {
    id: 'im_008', name: 'Paneer 200g', category: 'Dairy',
    price: 85, imageUrl: 'https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=200&q=80',
    inStock: true
  }
];

const DINEOUT_RESTAURANTS = [
  {
    id: 'do_001', name: 'The Rooftop Cafe', cuisines: ['Continental', 'Cafe'],
    rating: 4.6, costForTwo: 1200, area: 'Ahmedabad',
    imageUrl: 'https://images.unsplash.com/photo-1544739313-0fad7206497f?w=400&q=80',
    tags: ['rooftop', 'view', 'live-music']
  },
  {
    id: 'do_002', name: 'Toit Brewpub', cuisines: ['Continental', 'Italian', 'Brewery'],
    rating: 4.7, costForTwo: 2000, area: 'Bangalore',
    imageUrl: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&q=80',
    tags: ['brewery', 'night-out']
  },
  {
    id: 'do_003', name: 'Bastian', cuisines: ['Seafood', 'Asian'],
    rating: 4.8, costForTwo: 3500, area: 'Mumbai',
    imageUrl: 'https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?w=400&q=80',
    tags: ['premium', 'seafood']
  }
];

const MENU = {
  'r_f001': {
    restaurantId: 'r_f001',
    categories: [
      {
        name: 'Biryanis',
        items: [
          { id: 'm_001', name: 'Chicken Biryani', price: 280, isVeg: false, spiceLevel: 'medium' },
          { id: 'm_002', name: 'Mutton Biryani', price: 350, isVeg: false, spiceLevel: 'spicy' },
          { id: 'm_003', name: 'Veg Biryani', price: 220, isVeg: true, spiceLevel: 'mild' }
        ]
      }
    ]
  }
};

// ── Tool handlers ─────────────────────────────────────────────────────────────

function handleFood(toolName, args) {
  switch (toolName) {
    case 'get_addresses':
      return { addresses: ADDRESSES };

    case 'search_restaurants': {
      const query = (args.query || '').toLowerCase();
      const filtered = query
        ? FOOD_RESTAURANTS.filter(r =>
            r.name.toLowerCase().includes(query) ||
            r.cuisines.some(c => c.toLowerCase().includes(query)) ||
            r.tags.some(t => t.toLowerCase().includes(query))
          )
        : FOOD_RESTAURANTS;
      return { restaurants: filtered.length > 0 ? filtered : FOOD_RESTAURANTS };
    }

    case 'get_restaurant_menu': {
      const menu = MENU[args.restaurantId] || {
        restaurantId: args.restaurantId,
        categories: [
          { name: 'Main Course', items: [{ id: 'm_x01', name: 'Chef Special', price: 250, isVeg: false }] }
        ]
      };
      return menu;
    }

    case 'search_menu': {
      const query = (args.query || '').toLowerCase();
      const items = Object.values(MENU).flatMap(m =>
        m.categories.flatMap(c => c.items)
      ).filter(i => i.name.toLowerCase().includes(query));
      return { items };
    }

    case 'get_food_cart':
      return { items: [], total: 0, restaurantId: null };

    case 'update_food_cart':
      return { success: true, cart: { items: args.items || [], total: 0 } };

    case 'flush_food_cart':
      return { success: true };

    case 'fetch_food_coupons':
      return { coupons: [{ code: 'FIRST50', description: '50% off up to ₹100', requiresOnlinePayment: false }] };

    case 'apply_food_coupon':
      return { success: true, discount: 50 };

    case 'place_food_order':
      return { orderId: 'ord_' + Date.now(), status: 'PLACED', estimatedDelivery: 30 };

    case 'track_food_order':
      return { orderId: args.orderId, status: 'OUT_FOR_DELIVERY', etaMinutes: 15 };

    case 'get_food_orders':
      return { orders: [] };

    default:
      return null;
  }
}

function handleInstamart(toolName, args) {
  switch (toolName) {
    case 'get_addresses':
      return { addresses: ADDRESSES };

    case 'search_products': {
      const query = (args.query || '').toLowerCase();
      const filtered = query
        ? INSTAMART_PRODUCTS.filter(p =>
            p.name.toLowerCase().includes(query) ||
            p.category.toLowerCase().includes(query)
          )
        : INSTAMART_PRODUCTS;
      return { products: filtered.length > 0 ? filtered : INSTAMART_PRODUCTS };
    }

    case 'get_cart':
      return { items: [], total: 0 };

    case 'update_cart':
      return { success: true, cart: { items: args.items || [] } };

    case 'clear_cart':
      return { success: true };

    case 'checkout':
      return { orderId: 'im_ord_' + Date.now(), status: 'PLACED', estimatedDelivery: 20 };

    case 'track_order':
      return { orderId: args.orderId, status: 'PACKED', etaMinutes: 12 };

    case 'get_orders':
      return { orders: [] };

    case 'your_go_to_items':
      return { products: INSTAMART_PRODUCTS.slice(0, 3) };

    default:
      return null;
  }
}

function handleDineout(toolName, args) {
  switch (toolName) {
    case 'get_saved_locations':
      return { locations: ADDRESSES };

    case 'search_restaurants_dineout': {
      const query = (args.query || '').toLowerCase();
      const filtered = query
        ? DINEOUT_RESTAURANTS.filter(r =>
            r.name.toLowerCase().includes(query) ||
            r.cuisines.some(c => c.toLowerCase().includes(query)) ||
            r.tags.some(t => t.toLowerCase().includes(query))
          )
        : DINEOUT_RESTAURANTS;
      return { restaurants: filtered.length > 0 ? filtered : DINEOUT_RESTAURANTS };
    }

    case 'get_restaurant_details':
      return DINEOUT_RESTAURANTS.find(r => r.id === args.restaurantId) || DINEOUT_RESTAURANTS[0];

    case 'get_available_slots':
      return {
        slots: [
          { slotId: 'slot_1', time: '19:00', available: true, label: 'Dinner' },
          { slotId: 'slot_2', time: '20:00', available: true, label: 'Dinner' },
          { slotId: 'slot_3', time: '21:00', available: false, label: 'Dinner' }
        ]
      };

    case 'book_table':
      return { bookingId: 'bk_' + Date.now(), status: 'CONFIRMED', restaurantId: args.restaurantId };

    case 'get_booking_status':
      return { bookingId: args.bookingId, status: 'CONFIRMED' };

    default:
      return null;
  }
}

// ── Route factory ─────────────────────────────────────────────────────────────

function makeRoute(handler) {
  return (req, res) => {
    const { id, method, params } = req.body;

    if (method !== 'tools/call') {
      return res.json(err(id, -32601, `Method not supported: ${method}`));
    }

    const { name: toolName, arguments: args = {} } = params || {};
    const data = handler(toolName, args);

    if (data === null) {
      return res.json(err(id, -32602, `Unknown tool: ${toolName}`));
    }

    res.json(ok(id, data));
  };
}

// ── Routes ────────────────────────────────────────────────────────────────────

app.post('/food',    makeRoute(handleFood));
app.post('/im',      makeRoute(handleInstamart));
app.post('/dineout', makeRoute(handleDineout));

// Health check
app.get('/health', (_, res) => res.json({ status: 'ok', servers: ['food', 'im', 'dineout'] }));

// ── Start ─────────────────────────────────────────────────────────────────────

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`SwiggyMind MCP stub running on http://localhost:${PORT}`);
  console.log(`  Food:     POST http://localhost:${PORT}/food`);
  console.log(`  Instamart: POST http://localhost:${PORT}/im`);
  console.log(`  Dineout:  POST http://localhost:${PORT}/dineout`);
  console.log(`\nAndroid emulator → http://10.0.2.2:${PORT}`);
});
