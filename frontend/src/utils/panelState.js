// 简单的事件总线，用于组件间通信
import { ref } from 'vue';

const panelState = ref({
  activePanel: null // 'cart', 'favorites', or null
});

export const usePanelState = () => {
  const setActivePanel = (panel) => {
    panelState.value.activePanel = panel;
  };

  const clearActivePanel = () => {
    panelState.value.activePanel = null;
  };

  const isActivePanel = (panel) => {
    return panelState.value.activePanel === panel;
  };

  return {
    panelState,
    setActivePanel,
    clearActivePanel,
    isActivePanel
  };
};

