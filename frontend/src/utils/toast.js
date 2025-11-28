import Swal from 'sweetalert2';

const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  didOpen: (toast) => {
    toast.addEventListener('mouseenter', Swal.stopTimer);
    toast.addEventListener('mouseleave', Swal.resumeTimer);
  }
});

export default {
  success(message) {
    Toast.fire({
      icon: 'success',
      title: message
    });
  },
  error(message) {
    Toast.fire({
      icon: 'error',
      title: message
    });
  },
  warning(message) {
    Toast.fire({
      icon: 'warning',
      title: message
    });
  },
  info(message) {
    Toast.fire({
      icon: 'info',
      title: message
    });
  }
};
