// 商品分类数据（参考闲鱼）
export const categories = [
  {
    value: 'electronics',
    label: '📱 数码产品',
    icon: '📱',
    subcategories: [
      { value: 'phone', label: '手机' },
      { value: 'computer', label: '电脑/平板' },
      { value: 'camera', label: '相机/摄影' },
      { value: 'gaming', label: '游戏/电竞' },
      { value: 'accessories', label: '数码配件' }
    ]
  },
  {
    value: 'clothing',
    label: '👔 服装鞋包',
    icon: '👔',
    subcategories: [
      { value: 'men-clothing', label: '男装' },
      { value: 'women-clothing', label: '女装' },
      { value: 'shoes', label: '鞋子' },
      { value: 'bags', label: '箱包' },
      { value: 'accessories', label: '配饰' }
    ]
  },
  {
    value: 'books',
    label: '📚 图书音像',
    icon: '📚',
    subcategories: [
      { value: 'textbook', label: '教材教辅' },
      { value: 'literature', label: '文学小说' },
      { value: 'professional', label: '专业书籍' },
      { value: 'magazine', label: '杂志期刊' },
      { value: 'music', label: '音乐影视' }
    ]
  },
  {
    value: 'beauty',
    label: '💄 美妆个护',
    icon: '💄',
    subcategories: [
      { value: 'skincare', label: '护肤品' },
      { value: 'makeup', label: '彩妆' },
      { value: 'perfume', label: '香水' },
      { value: 'personal-care', label: '个人护理' }
    ]
  },
  {
    value: 'home',
    label: '🏠 家居生活',
    icon: '🏠',
    subcategories: [
      { value: 'furniture', label: '家具' },
      { value: 'decoration', label: '家居饰品' },
      { value: 'kitchen', label: '厨具餐具' },
      { value: 'bedding', label: '床上用品' },
      { value: 'appliances', label: '家用电器' }
    ]
  },
  {
    value: 'sports',
    label: '⚽ 运动户外',
    icon: '⚽',
    subcategories: [
      { value: 'fitness', label: '健身器材' },
      { value: 'sports-wear', label: '运动服饰' },
      { value: 'outdoor', label: '户外装备' },
      { value: 'bicycle', label: '自行车' }
    ]
  },
  {
    value: 'baby',
    label: '👶 母婴用品',
    icon: '👶',
    subcategories: [
      { value: 'toys', label: '玩具' },
      { value: 'clothing', label: '童装' },
      { value: 'feeding', label: '喂养用品' },
      { value: 'stroller', label: '推车座椅' }
    ]
  },
  {
    value: 'food',
    label: '🍔 食品饮料',
    icon: '🍔',
    subcategories: [
      { value: 'snacks', label: '零食特产' },
      { value: 'health-food', label: '保健食品' },
      { value: 'tea', label: '茶叶' },
      { value: 'alcohol', label: '酒类' }
    ]
  },
  {
    value: 'jewelry',
    label: '💎 珠宝首饰',
    icon: '💎',
    subcategories: [
      { value: 'necklace', label: '项链' },
      { value: 'ring', label: '戒指' },
      { value: 'bracelet', label: '手链手镯' },
      { value: 'watch', label: '手表' }
    ]
  },
  {
    value: 'vehicles',
    label: '🚗 交通工具',
    icon: '🚗',
    subcategories: [
      { value: 'car', label: '汽车' },
      { value: 'motorcycle', label: '摩托车' },
      { value: 'ebike', label: '电动车' },
      { value: 'parts', label: '配件' }
    ]
  },
  {
    value: 'pets',
    label: '🐶 宠物用品',
    icon: '🐶',
    subcategories: [
      { value: 'food', label: '宠物食品' },
      { value: 'supplies', label: '宠物用品' },
      { value: 'toys', label: '宠物玩具' }
    ]
  },
  {
    value: 'other',
    label: '🎁 其他闲置',
    icon: '🎁',
    subcategories: [
      { value: 'tickets', label: '票券' },
      { value: 'cards', label: '卡券' },
      { value: 'collectibles', label: '收藏品' },
      { value: 'other', label: '其他' }
    ]
  }
];

// 获取所有主分类
export const getMainCategories = () => {
  return categories.map(cat => ({
    value: cat.value,
    label: cat.label,
    icon: cat.icon
  }));
};

// 根据主分类获取子分类
export const getSubCategories = (mainCategory) => {
  const category = categories.find(cat => cat.value === mainCategory);
  return category ? category.subcategories : [];
};

// 根据分类值获取分类名称
export const getCategoryLabel = (categoryValue) => {
  for (const category of categories) {
    if (category.value === categoryValue) {
      return category.label;
    }
    const subCategory = category.subcategories.find(sub => sub.value === categoryValue);
    if (subCategory) {
      return `${category.label} / ${subCategory.label}`;
    }
  }
  return '未分类';
};

// 获取分类图标
export const getCategoryIcon = (categoryValue) => {
  const category = categories.find(cat => cat.value === categoryValue);
  return category ? category.icon : '🎁';
};

