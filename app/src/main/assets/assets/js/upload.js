$(function() {
	function maskImgs() {
		$.each($('.img-wrapper img'), function(index, img) {
			var src = $(img).attr('src');
			var parent = $(img).parent();
			parent
				.css('background', 'url(' + src + ') no-repeat center center')
				.css('background-size', 'cover');
			$(img).remove();
		});
	}

	var preview = {
		init: function() {
			preview.setPreviewImg();
			preview.listenInput();
		},
		setPreviewImg: function(fileInput) {
			var path = $(fileInput).val();
			var uploadText = $(fileInput).siblings('.file-upload-text');

			if (!path) {
				$(uploadText).val('');
			} else {
				path = path.replace(/^C:\\fakepath\\/, "");
				$(uploadText).val(path);

				preview.showPreview(fileInput, path, uploadText);
			}
		},
		showPreview: function(fileInput, path, uploadText) {
			var file = $(fileInput)[0].files;

			if (file && file[0]) {
				var reader = new FileReader();

				reader.onload = function(e) {
					var previewImg = $(fileInput).parents('.file-upload-wrapper').siblings('.preview');
					var img = $(previewImg).find('img');

					if (img.length == 0) {
						$(previewImg).html('<img src="' + e.target.result + '" alt=""/>');
					} else {
						img.attr('src', e.target.result);
					}

					uploadText.val(path);
					
					
					$("#uploadImg").prop("disabled", true);
					
					
					Swal.fire({
						title: 'Please Wait...',
						html: 'AI가 당신의 거북목을 진단하고 있습니다.',
						allowOutsideClick: false,
						allowEscapeKey: false,
						showCloseButton: false,
						showCancelButton: false,
						showConfirmButton: false,
						
						onBeforeOpen: () => {
							Swal.showLoading();
						}
					});
					
					
					init().then(async function() {
						await predict();
						Swal.close();
						
						
						var toastMixin = await Swal.mixin({
							toast: true,
							icon: 'success',
							title: 'General Title',
							animation: true,
							position: 'bottom-right',
							showConfirmButton: false,
							timer: 3000,
							timerProgressBar: true,
						});
						
						
						await toastMixin.fire({
							animation: true,
							title: '사진 분석 완료! 잠시 후 결과가 출력됩니다.'
						});
						
						
						$("#uploadImg").prop("disabled", false);
						
						
						location.href="result.html?result=" + resultImg + "&pb1=" + result_tn_nom_v + "&pb2=" + result_tn_abn_v;
					});
					
					maskImgs();
				}

				reader.onloadstart = function() {
					$(uploadText).val('uploading..');
				}

				reader.readAsDataURL(file[0]);
			}
		},
		listenInput: function() {
			$('.file-upload-native').on('change', function() {
				preview.setPreviewImg(this);
			});
		}
	};
	preview.init();
});