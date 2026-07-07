import threading
from typing import Any


class Singleton:
    _instances: dict[type, Any] = {}
    _global_lock = threading.Lock()

    def __new__(cls, *args, **kwargs) -> Any:
        if cls not in Singleton._instances:
            # 每个 Singleton 子类使用独立的类锁，避免不同子类互相阻塞
            cls_lock = cls._get_class_lock()
            with cls_lock:
                if cls not in Singleton._instances:
                    instance = super().__new__(cls)
                    instance._initialized = False
                    Singleton._instances[cls] = instance
        return Singleton._instances[cls]

    @classmethod
    def _get_class_lock(cls):
        """获取或创建类级别的锁，使不同 Singleton 子类互不阻塞"""
        if not hasattr(cls, '_cls_lock'):
            with Singleton._global_lock:
                if not hasattr(cls, '_cls_lock'):
                    cls._cls_lock = threading.Lock()
        return cls._cls_lock

    @classmethod
    def reset_instance(cls):
        """重置单例实例（仅用于测试，生产环境调用可能导致持有旧引用的代码出现不可预期行为）"""
        cls_lock = cls._get_class_lock()
        with cls_lock:
            instance = Singleton._instances.pop(cls, None)
            if instance is not None:
                instance._initialized = False
