$(function() {

  var $variant = $('#form3-variant');
  var $position = $('.form3 .position');
  function showPosition() {
    $position.toggleNone($variant.val() == 1);
  };
  $variant.on('change', showPosition);
  showPosition();

  $('.tour__form .conditions a.show').on('click', function() {
    $(this).remove();
    $('.tour__form .conditions').addClass('visible');
  });

  $(".tour__form .flatpickr").flatpickr({
    minDate: 'today',
    maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
    dateFormat: 'Z',
    altInput: true,
    altFormat: 'Y-m-d h:i K'
  });

//     if (topicId) lichess.loadScript('vendor/textcomplete.min.js').then(function() {

//       var searchCandidates = function(term, candidateUsers) {
//         return candidateUsers.filter(function(user) {
//           return user.toLowerCase().startsWith(term.toLowerCase());
//         });
//       };

//       // We only ask the server for the thread participants once the user has clicked the text box as most hits to the
//       // forums will be only to read the thread. So the 'thread participants' starts out empty until the post text area
//       // is focused.
//       var threadParticipants = $.ajax({
//         url: "/forum/participants/" + topicId
//       });

//       var textcomplete = new Textcomplete(new Textcomplete.editors.Textarea(textarea));

//       textcomplete.register([{
//         match: /(^|\s)@(|[a-zA-Z_-][\w-]{0,19})$/,
//         search: function(term, callback) {

//           // Initially we only autocomplete by participants in the thread. As the user types more,
//           // we can autocomplete against all users on the site.
//           threadParticipants.then(function(participants) {
//             var forumParticipantCandidates = searchCandidates(term, participants);

//             if (forumParticipantCandidates.length != 0) {
//               // We always prefer a match on the forum thread partcipants' usernames
//               callback(forumParticipantCandidates);
//             }
//             else if (term.length >= 3) {
//               // We fall back to every site user after 3 letters of the username have been entered
//               // and there are no matches in the forum thread participants
//               $.ajax({
//                 url: "/player/autocomplete",
//                 data: {
//                   term: term
//                 },
//                 success: function(candidateUsers) {
//                   callback(searchCandidates(term, candidateUsers));
//                 },
//                 cache: true
//               });
//             } else {
//               callback([]);
//             }
//           });
//         },
//         replace: function(mention) {
//           return '$1@' + mention + ' ';
//         }
//       }], {
//         placement: 'top',
//         appendTo: '#lichess_forum'
//       });
//     });
//   });
});
